/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.optimization.boxing;

import com.intellij.openapi.util.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue;
import org.jetbrains.kotlin.codegen.optimization.common.UtilKt;
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame;

import java.util.*;

public class RedundantBoxingMethodTransformer extends MethodTransformer {

    @Override
    public void transform(@NotNull String internalClassName, @NotNull MethodNode node) {
        RedundantBoxingInterpreter interpreter = new RedundantBoxingInterpreter(node.instructions);
        Frame<BasicValue>[] frames = analyze(internalClassName, node, interpreter);

        interpretPopInstructionsForBoxedValues(interpreter, node, frames);

        RedundantBoxedValuesCollection valuesToOptimize = interpreter.getCandidatesBoxedValues();

        if (!valuesToOptimize.isEmpty()) {
            // has side effect on valuesToOptimize and frames, containing BoxedBasicValues that are unsafe to remove
            removeValuesClashingWithVariables(valuesToOptimize, node, frames);

            adaptLocalVariableTableForBoxedValues(node, frames);

            UtilKt.remapLocalVariables(node, buildVariablesRemapping(valuesToOptimize, node));

            adaptInstructionsForBoxedValues(node, valuesToOptimize);
        }
    }

    private static void interpretPopInstructionsForBoxedValues(
            @NotNull RedundantBoxingInterpreter interpreter,
            @NotNull MethodNode node,
            @NotNull Frame<BasicValue>[] frames
    ) {
        for (int i = 0; i < node.instructions.size(); i++) {
            AbstractInsnNode insn = node.instructions.get(i);
            if ((insn.getOpcode() != Opcodes.POP && insn.getOpcode() != Opcodes.POP2) || frames[i] == null) {
                continue;
            }

            BasicValue top = frames[i].getStack(frames[i].getStackSize() - 1);
            interpreter.processPopInstruction(insn, top);

            if (top.getSize() == 1 && insn.getOpcode() == Opcodes.POP2) {
                interpreter.processPopInstruction(insn, frames[i].getStack(frames[i].getStackSize() - 2));
            }
        }
    }

    private static void removeValuesClashingWithVariables(
            @NotNull RedundantBoxedValuesCollection values,
            @NotNull MethodNode node,
            @NotNull Frame<BasicValue>[] frames
    ) {
        while (removeValuesClashingWithVariablesPass(values, node, frames)) {
            // do nothing
        }
    }

    private static boolean removeValuesClashingWithVariablesPass(
            @NotNull RedundantBoxedValuesCollection values,
            @NotNull MethodNode node,
            @NotNull Frame<BasicValue>[] frames
    ) {
        boolean needToRepeat = false;

        for (LocalVariableNode localVariableNode : node.localVariables) {
            if (Type.getType(localVariableNode.desc).getSort() != Type.OBJECT) {
                continue;
            }

            List<BasicValue> variableValues = getValuesStoredOrLoadedToVariable(localVariableNode, node, frames);

            Collection<BasicValue> boxed = CollectionsKt.filter(variableValues, new Function1<BasicValue, Boolean>() {
                @Override
                public Boolean invoke(BasicValue value) {
                    return value instanceof BoxedBasicValue;
                }
            });

            if (boxed.isEmpty()) continue;

            BoxedValueDescriptor firstBoxed = ((BoxedBasicValue) boxed.iterator().next()).getDescriptor();
            if (isUnsafeToRemoveBoxingForConnectedValues(variableValues, firstBoxed.getUnboxedType())) {
                for (BasicValue value : variableValues) {
                    if (!(value instanceof BoxedBasicValue)) continue;

                    BoxedValueDescriptor descriptor = ((BoxedBasicValue) value).getDescriptor();
                    if (descriptor.isSafeToRemove()) {
                        values.remove(descriptor);
                        needToRepeat = true;
                    }
                }
            }
        }

        return needToRepeat;
    }

    private static boolean isUnsafeToRemoveBoxingForConnectedValues(List<BasicValue> usedValues, final Type unboxedType) {
        return CollectionsKt.any(usedValues, new Function1<BasicValue, Boolean>() {
            @Override
            public Boolean invoke(BasicValue input) {
                if (input == StrictBasicValue.UNINITIALIZED_VALUE) return false;
                if (!(input instanceof BoxedBasicValue)) return true;

                BoxedValueDescriptor descriptor = ((BoxedBasicValue) input).getDescriptor();
                return !descriptor.isSafeToRemove() ||
                       !(descriptor.getUnboxedType().equals(unboxedType));
            }
        });
    }

    private static void adaptLocalVariableTableForBoxedValues(@NotNull MethodNode node, @NotNull Frame<BasicValue>[] frames) {
        for (LocalVariableNode localVariableNode : node.localVariables) {
            if (Type.getType(localVariableNode.desc).getSort() != Type.OBJECT) {
                continue;
            }

            for (BasicValue value : getValuesStoredOrLoadedToVariable(localVariableNode, node, frames)) {
                if (!(value instanceof BoxedBasicValue)) continue;

                BoxedValueDescriptor descriptor = ((BoxedBasicValue) value).getDescriptor();
                if (!descriptor.isSafeToRemove()) continue;
                localVariableNode.desc = descriptor.getUnboxedType().getDescriptor();
            }
        }
    }

    @NotNull
    private static List<BasicValue> getValuesStoredOrLoadedToVariable(
            @NotNull LocalVariableNode localVariableNode,
            @NotNull MethodNode node,
            @NotNull Frame<BasicValue>[] frames
    ) {
        List<BasicValue> values = new ArrayList<BasicValue>();
        InsnList insnList = node.instructions;
        int from = insnList.indexOf(localVariableNode.start) + 1;
        int to = insnList.indexOf(localVariableNode.end) - 1;

        Frame<BasicValue> frameForFromInstr = frames[from];
        if (frameForFromInstr != null) {
            BasicValue localVarValue = frameForFromInstr.getLocal(localVariableNode.index);
            if (localVarValue != null) {
                values.add(localVarValue);
            }
        }

        for (int i = from; i <= to; i++) {
            if (i < 0 || i >= insnList.size()) continue;

            AbstractInsnNode insn = insnList.get(i);
            if ((insn.getOpcode() == Opcodes.ASTORE || insn.getOpcode() == Opcodes.ALOAD) &&
                ((VarInsnNode) insn).var == localVariableNode.index) {

                if (frames[i] == null) {
                    //unreachable code
                    continue;
                }

                if (insn.getOpcode() == Opcodes.ASTORE) {
                    values.add(frames[i].getStack(frames[i].getStackSize() - 1));
                }
                else {
                    values.add(frames[i].getLocal(((VarInsnNode) insn).var));
                }
            }
        }

        return values;
    }

    @NotNull
    private static int[] buildVariablesRemapping(@NotNull RedundantBoxedValuesCollection values, @NotNull MethodNode node) {
        Set<Integer> doubleSizedVars = new HashSet<Integer>();
        for (BoxedValueDescriptor valueDescriptor : values) {
            if (valueDescriptor.isDoubleSize()) {
                doubleSizedVars.addAll(valueDescriptor.getVariablesIndexes());
            }
        }

        node.maxLocals += doubleSizedVars.size();
        int[] remapping = new int[node.maxLocals];
        for (int i = 0; i < remapping.length; i++) {
            remapping[i] = i;
        }

        for (int varIndex : doubleSizedVars) {
            for (int i = varIndex + 1; i < remapping.length; i++) {
                remapping[i]++;
            }
        }

        return remapping;
    }

    private static void adaptInstructionsForBoxedValues(
            @NotNull MethodNode node,
            @NotNull RedundantBoxedValuesCollection values
    ) {
        for (BoxedValueDescriptor value : values) {
            adaptInstructionsForBoxedValue(node, value);
        }
    }

    private static void adaptInstructionsForBoxedValue(@NotNull MethodNode node, @NotNull BoxedValueDescriptor value) {
        adaptBoxingInstruction(node, value);

        for (Pair<AbstractInsnNode, Type> cast : value.getUnboxingWithCastInsns()) {
            adaptCastInstruction(node, value, cast);
        }

        for (AbstractInsnNode insn : value.getAssociatedInsns()) {
            adaptInstruction(node, insn, value);
        }
    }

    private static void adaptBoxingInstruction(@NotNull MethodNode node, @NotNull BoxedValueDescriptor value) {
        if (!value.isFromProgressionIterator()) {
            node.instructions.remove(value.getBoxingInsn());
        }
        else {
            ProgressionIteratorBasicValue iterator = value.getProgressionIterator();
            assert iterator != null : "iterator should not be null because isFromProgressionIterator returns true";

            //add checkcast to kotlin/<T>Iterator before next() call
            node.instructions.insertBefore(
                    value.getBoxingInsn(),
                    new TypeInsnNode(Opcodes.CHECKCAST, iterator.getType().getInternalName())
            );

            //invoke concrete method (kotlin/<T>iterator.next<T>())
            node.instructions.set(
                    value.getBoxingInsn(),
                    new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            iterator.getType().getInternalName(),
                            iterator.getNextMethodName(),
                            iterator.getNextMethodDesc(),
                            false
                    )
            );
        }
    }

    private static void adaptCastInstruction(
            @NotNull MethodNode node,
            @NotNull BoxedValueDescriptor value,
            @NotNull Pair<AbstractInsnNode, Type> castWithType
    ) {
        AbstractInsnNode castInsn = castWithType.getFirst();
        MethodNode castInsnsListener = new MethodNode(Opcodes.ASM5);
        new InstructionAdapter(castInsnsListener).cast(value.getUnboxedType(), castWithType.getSecond());

        for (AbstractInsnNode insn : castInsnsListener.instructions.toArray()) {
            node.instructions.insertBefore(castInsn, insn);
        }

        node.instructions.remove(castInsn);
    }

    private static void adaptInstruction(
            @NotNull MethodNode node, @NotNull AbstractInsnNode insn, @NotNull BoxedValueDescriptor value
    ) {
        boolean isDoubleSize = value.isDoubleSize();

        switch (insn.getOpcode()) {
            case Opcodes.POP:
                if (isDoubleSize) {
                    node.instructions.set(
                            insn,
                            new InsnNode(Opcodes.POP2)
                    );
                }
                break;
            case Opcodes.DUP:
                if (isDoubleSize) {
                    node.instructions.set(
                            insn,
                            new InsnNode(Opcodes.DUP2)
                    );
                }
                break;
            case Opcodes.ASTORE:
            case Opcodes.ALOAD:
                int intVarOpcode = insn.getOpcode() == Opcodes.ASTORE ? Opcodes.ISTORE : Opcodes.ILOAD;
                node.instructions.set(
                        insn,
                        new VarInsnNode(
                                value.getUnboxedType().getOpcode(intVarOpcode),
                                ((VarInsnNode) insn).var
                        )
                );
                break;
            case Opcodes.INSTANCEOF:
                node.instructions.insertBefore(
                        insn,
                        new InsnNode(isDoubleSize ? Opcodes.POP2 : Opcodes.POP)
                );
                node.instructions.set(insn, new InsnNode(Opcodes.ICONST_1));
                break;
            case Opcodes.INVOKESTATIC:
                if (BoxingInterpreterKt.isAreEqualIntrinsic(insn)) {
                    adaptAreEqualIntrinsic(node, insn, value);
                    break;
                }
                else {
                    // fall-through to default
                }
            default:
                // CHECKCAST or unboxing-method call
                node.instructions.remove(insn);
        }
    }

    private static void adaptAreEqualIntrinsic(@NotNull MethodNode node, @NotNull AbstractInsnNode insn, @NotNull BoxedValueDescriptor value) {
        Type unboxedType = value.getUnboxedType();

        switch (unboxedType.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.CHAR:
                adaptAreEqualIntrinsicForInt(node, insn);
                break;
            case Type.LONG:
                adaptAreEqualIntrinsicForLong(node, insn);
                break;
            case Type.OBJECT:
                break;
            default:
                throw new AssertionError("Unexpected unboxed type kind: " + unboxedType);
        }
    }

    private static void adaptAreEqualIntrinsicForInt(@NotNull MethodNode node, @NotNull AbstractInsnNode insn) {
        LabelNode lNotEqual = new LabelNode(new Label());
        LabelNode lDone = new LabelNode(new Label());
        node.instructions.insertBefore(insn, new JumpInsnNode(Opcodes.IF_ICMPNE, lNotEqual));
        node.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_1));
        node.instructions.insertBefore(insn, new JumpInsnNode(Opcodes.GOTO, lDone));
        node.instructions.insertBefore(insn, lNotEqual);
        node.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_0));
        node.instructions.insertBefore(insn, lDone);

        node.instructions.remove(insn);
    }

    private static void adaptAreEqualIntrinsicForLong(@NotNull MethodNode node, @NotNull AbstractInsnNode insn) {
        node.instructions.insertBefore(insn, new InsnNode(Opcodes.LCMP));
        ifEqual1Else0(node, insn);
        node.instructions.remove(insn);
    }

    private static void ifEqual1Else0(@NotNull MethodNode node, @NotNull AbstractInsnNode insn) {
        LabelNode lNotEqual = new LabelNode(new Label());
        LabelNode lDone = new LabelNode(new Label());
        node.instructions.insertBefore(insn, new JumpInsnNode(Opcodes.IFNE, lNotEqual));
        node.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_1));
        node.instructions.insertBefore(insn, new JumpInsnNode(Opcodes.GOTO, lDone));
        node.instructions.insertBefore(insn, lNotEqual);
        node.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_0));
        node.instructions.insertBefore(insn, lDone);
    }
}

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

package org.jetbrains.kotlin.cfg;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableInitState;
import static org.jetbrains.kotlin.cfg.PseudocodeVariablesData.VariableUseState;

public abstract class AbstractDataFlowTest extends AbstractPseudocodeTest {

    @Override
    public void dumpInstructions(
            @NotNull PseudocodeImpl pseudocode,
            @NotNull StringBuilder out,
            @NotNull BindingContext bindingContext
    ) {
        PseudocodeVariablesData pseudocodeVariablesData = new PseudocodeVariablesData(pseudocode.getRootPseudocode(), bindingContext);
        final Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> variableInitializers =
                pseudocodeVariablesData.getVariableInitializers();
        final Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> useStatusData =
                pseudocodeVariablesData.getVariableUseStatusData();
        final String initPrefix = "    INIT:";
        final String usePrefix  = "    USE:";
        final int initializersColumnWidth = countDataColumnWidth(initPrefix, pseudocode.getInstructionsIncludingDeadCode(), variableInitializers);

        dumpInstructions(pseudocode, out, new Function3<Instruction, Instruction, Instruction, String>() {
            @Override
            public String invoke(Instruction instruction, Instruction next, Instruction prev) {
                StringBuilder result = new StringBuilder();
                Edges<Map<VariableDescriptor, VariableInitState>> initializersEdges = variableInitializers.get(instruction);
                Edges<Map<VariableDescriptor, VariableInitState>> previousInitializersEdges = variableInitializers.get(prev);
                String initializersData = "";
                if (initializersEdges != null && !initializersEdges.equals(previousInitializersEdges)) {
                    initializersData = dumpEdgesData(initPrefix, initializersEdges);
                }
                result.append(String.format("%1$-" + initializersColumnWidth + "s", initializersData));

                Edges<Map<VariableDescriptor, VariableUseState>> useStatusEdges = useStatusData.get(instruction);
                Edges<Map<VariableDescriptor, VariableUseState>> nextUseStatusEdges = useStatusData.get(next);
                if (useStatusEdges != null && !useStatusEdges.equals(nextUseStatusEdges)) {
                    result.append(dumpEdgesData(usePrefix, useStatusEdges));
                }
                return result.toString();
            }
        });
    }

    private static int countDataColumnWidth(
            @NotNull String prefix,
            @NotNull List<Instruction> instructions,
            @NotNull Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> data
    ) {
        int maxWidth = 0;
        for (Instruction instruction : instructions) {
            Edges<Map<VariableDescriptor, VariableInitState>> edges = data.get(instruction);
            if (edges == null) continue;
            int length = dumpEdgesData(prefix, edges).length();
            if (maxWidth < length) {
                maxWidth = length;
            }
        }
        return maxWidth;
    }

    @NotNull
    private static <D> String dumpEdgesData(String prefix, @NotNull Edges<Map<VariableDescriptor, D>> edges) {
        return prefix +
               " in: " + renderVariableMap(edges.getIncoming()) +
               " out: " + renderVariableMap(edges.getOutgoing());
    }

    private static <D> String renderVariableMap(Map<VariableDescriptor, D> map) {
        List<String> result = Lists.newArrayList();
        for (Map.Entry<VariableDescriptor, D> entry : map.entrySet()) {
            VariableDescriptor variable = entry.getKey();
            D data = entry.getValue();
            result.add(variable.getName() + "=" + data);
        }
        Collections.sort(result);
        return "{" + StringUtil.join(result, ", ") + "}";
    }
}

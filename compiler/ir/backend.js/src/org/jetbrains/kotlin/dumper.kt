/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

fun IrElement.dumpKotlinLike(): String {
    val sb = StringBuilder()
    acceptVoid(Dumper(Printer(sb, "  ")))
    return sb.toString()
}

class Dumper(val p: Printer) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        val e = "/*element: " + element.javaClass.simpleName + "*/"
        if (element is IrExpression) p.printWithNoIndent(e) else p.print(e)
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
        declaration.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        p.println("/// FILE: ${declaration.fileEntry.name}")
        val fileAnnotations = declaration.fileAnnotations
        if (fileAnnotations.isNotEmpty()) {
            p.println("@file:[${fileAnnotations.joinToString { it.type.constructor.declarationDescriptor!!.name.asString() + "(...)" }}]")
        }
        val packageFqName = declaration.packageFragmentDescriptor.fqName
        if (!packageFqName.isRoot) {
            p.println("package ${packageFqName.asString()}")
        }
        p.printlnWithNoIndent()

        declaration.declarations.forEach { it.acceptVoid(this) }
    }

    override fun visitFunction(declaration: IrFunction) {
        p.print("fun ${declaration.descriptor.name.asString()}(...) ")
        declaration.body?.acceptVoid(this)
        p.printlnWithNoIndent()
    }

    override fun visitProperty(declaration: IrProperty) {
        p.print(declaration.descriptor.headerAsString())

        declaration.backingField?.initializer?.let {
            p.printWithNoIndent(" = ")
            it.acceptVoid(this)
        }
        p.printlnWithNoIndent()

        p.pushIndent()

        declaration.getter?.printAccessor("get")
        declaration.setter?.printAccessor("set")
        p.popIndent()

        p.printlnWithNoIndent()
    }

    private fun VariableDescriptor.headerAsString() = "${kindAsString()} ${name.asString()}"

    private fun VariableDescriptor.kindAsString() = if (isVar) "var" else "val"

    private fun KotlinType.asString() = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.renderType(this)

    private fun IrFunction.printAccessor(s: String) {
        p.print(s)
        if (origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
            p.printWithNoIndent("() ")
            body?.acceptVoid(this@Dumper)
        }
        p.println()
    }

    override fun visitExpressionBody(body: IrExpressionBody) {
        body.expression.acceptVoid(this)
    }

    override fun visitBlockBody(body: IrBlockBody) {
        printStatementContainer(body, "{", "}")
        p.println()
    }

    private fun printStatementContainer(body: IrStatementContainer, before: String, after: String, withIndentation: Boolean = true) {
        p.printlnWithNoIndent(before)
        if (withIndentation) p.pushIndent()

        body.statements.forEach {
            if (it is IrExpression) p.printIndent()
            it.acceptVoid(this)
            p.printlnWithNoIndent()
        }

        if (withIndentation) p.popIndent()
        p.print(after)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody) {
        p.printlnWithNoIndent("/* SYNTHETIC BODY */")
    }

    override fun visitCall(expression: IrCall) {
        val descriptor = expression.descriptor

        expression.dispatchReceiver?.let {
            it.acceptVoid(this)
            p.printWithNoIndent(".")
        }

        p.printWithNoIndent(descriptor.name.asString())

        if (descriptor.original.typeParameters.isNotEmpty()) {
            p.printWithNoIndent("<")
            descriptor.original.typeParameters.forEach {
                p.printWithNoIndent(expression.getTypeArgument(it)!!.asString())
            }
            p.printWithNoIndent(">")
        }

        p.printWithNoIndent("(")
        expression.extensionReceiver?.let {
            p.printWithNoIndent("\$receiver = ")
            it.acceptVoid(this)
            if (descriptor.valueParameters.isNotEmpty()) p.printWithNoIndent(", ")
        }

        descriptor.valueParameters.forEachIndexed { i, pd ->
            expression.getValueArgument(pd)?.let {
                if (i > 0) p.printWithNoIndent(", ")
                p.printWithNoIndent(pd.name.asString() + " = ")
                it.acceptVoid(this)
            }
        }

        p.printWithNoIndent(")")
    }

    override fun visitGetField(expression: IrGetField) {
        p.printWithNoIndent("$" + expression.descriptor.name.asString())
    }

    override fun visitReturn(expression: IrReturn) {
        p.printWithNoIndent("return ")
        expression.value.acceptVoid(this)
    }

    override fun visitThrow(expression: IrThrow) {
        p.printWithNoIndent("throw ")
        expression.value.acceptVoid(this)
    }

    override fun visitComposite(expression: IrComposite) {
        printStatementContainer(expression, "// COMPOSITE {", "// }", withIndentation = false)
    }

    override fun visitBlock(expression: IrBlock) {
        printStatementContainer(expression, "{ //BLOCK", "}")
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        // TODO escape? see IrTextTestCaseGenerated.Expressions#testStringTemplates
        expression.arguments.forEachIndexed { i, e ->
            if (i > 0) {
                p.printlnWithNoIndent(" + ")
            }
            e.acceptVoid(this)
        }
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        val kind = expression.kind

        val (prefix, postfix) = when (kind) {
            is IrConstKind.Null -> "" to ""
            is IrConstKind.Boolean -> "" to ""
            is IrConstKind.Char -> "'" to "'"
            is IrConstKind.Byte -> "" to "B"
            is IrConstKind.Short -> "" to "S"
            is IrConstKind.Int -> "" to ""
            is IrConstKind.Long -> "" to "L"
            is IrConstKind.String -> "\"" to "\""
            is IrConstKind.Float -> "" to "F"
            is IrConstKind.Double -> "" to "D"
        }

        p.printWithNoIndent(prefix, expression.value ?: "null", postfix)
    }

    override fun visitClass(declaration: IrClass) {
        val descriptor = declaration.descriptor

        val kind = DescriptorRenderer.getClassifierKindPrefix(descriptor)

        p.println(kind + " " + descriptor.name.asString() + " {")
        p.pushIndent()

        declaration.declarations.forEach { it.acceptVoid(this) }

        p.popIndent()
        p.println("}")
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        p.println("typealias " + declaration.descriptor.name.asString())
    }

    override fun visitConstructor(declaration: IrConstructor) {
        // TODO render signature
        p.print("constructor() ")
        declaration.body?.acceptVoid(this)
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        super.visitLocalDelegatedProperty(declaration)
    }

    override fun visitVariable(declaration: IrVariable) {
        p.print(declaration.descriptor.headerAsString())
        declaration.initializer?.let {
            p.printWithNoIndent(" = ")
            it.acceptVoid(this)
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        super.visitEnumEntry(declaration)
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        super.visitAnonymousInitializer(declaration)
    }

    override fun visitVararg(expression: IrVararg) {
        p.printWithNoIndent("[")
        expression.elements.forEachIndexed { i, e ->
            if (i > 0) p.printWithNoIndent(", ")
            e.acceptVoid(this)
        }
        p.printWithNoIndent("]")
    }

    override fun visitSpreadElement(spread: IrSpreadElement) {
        p.printWithNoIndent("*")
        spread.expression.acceptVoid(this)
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)
    }

    override fun visitSingletonReference(expression: IrGetSingletonValue) {
        p.printWithNoIndent(expression.descriptor.defaultType.asString())
    }

    override fun visitGetValue(expression: IrGetValue) {
        // TODO support `this` and receiver
        p.printWithNoIndent(expression.descriptor.name.asString())
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        p.printWithNoIndent(expression.descriptor.name.asString() +  " = ")
        expression.value.acceptVoid(this)
    }

    override fun visitSetField(expression: IrSetField) {
        super.visitSetField(expression)
    }

    override fun visitGetClass(expression: IrGetClass) {
        expression.argument.acceptVoid(this)
        p.printWithNoIndent("::class")
    }

    override fun visitCallableReference(expression: IrCallableReference) {
        // TODO
        p.printWithNoIndent("::")
        p.printWithNoIndent(expression.descriptor.name.asString())
    }

    override fun visitClassReference(expression: IrClassReference) {
        p.printWithNoIndent(expression.descriptor.defaultType.asString())
        p.printWithNoIndent("::class")
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        p.println("/* InstanceInitializerCall */")
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        val (operator, after) = when (expression.operator) {
            IrTypeOperator.CAST -> "as" to ""
            IrTypeOperator.IMPLICIT_CAST -> "/*as" to " */"
            IrTypeOperator.IMPLICIT_NOTNULL -> "/*!!" to " */"
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> "/*as" to " */"
            IrTypeOperator.SAFE_CAST -> "as?" to ""
            IrTypeOperator.INSTANCEOF -> "is" to ""
            IrTypeOperator.NOT_INSTANCEOF -> "!is" to ""
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> TODO()
        }

        expression.argument.acceptVoid(this)
        p.printWithNoIndent(" $operator ")
        p.printWithNoIndent(expression.typeOperand.asString())
        p.printWithNoIndent(after)

    }

    override fun visitWhen(expression: IrWhen) {
        p.printlnWithNoIndent("when {")
        p.pushIndent()

        for (b in expression.branches) {
            p.printIndent()
            b.condition.acceptVoid(this)

            p.printWithNoIndent(" -> ")

            b.result.acceptVoid(this)

            p.println()
        }

        p.popIndent()
        p.print("}")
    }

    override fun visitWhileLoop(loop: IrWhileLoop) {
        p.printWithNoIndent("while (")
        loop.condition.acceptVoid(this)

        p.printWithNoIndent(") ")

        loop.body?.acceptVoid(this)
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        p.printWithNoIndent("do")

        loop.body?.acceptVoid(this)

        p.print("while (")
        loop.condition.acceptVoid(this)
        p.printWithNoIndent(")")

    }

    override fun visitTry(aTry: IrTry) {
        p.printWithNoIndent("try ")
        aTry.tryResult.acceptVoid(this)
        p.printlnWithNoIndent()

        aTry.catches.forEach { it.acceptVoid(this) }

        aTry.finallyExpression?.let {
            p.print("finally ")
            it.acceptVoid(this)
        }
    }

    override fun visitCatch(aCatch: IrCatch) {
        p.print("catch (...) ")
        aCatch.result.acceptVoid(this)
        p.printlnWithNoIndent()
    }

    override fun visitBreak(jump: IrBreak) {
        // TODO label
        p.printWithNoIndent("break")
    }

    override fun visitContinue(jump: IrContinue) {
        p.printWithNoIndent("continue")
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration) {
        p.println("/* ERROR DECLARATION */")
    }

    override fun visitErrorExpression(expression: IrErrorExpression) {
        p.println("/* ERROR EXPRESSION */")
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression) {
        p.println("/* ERROR CALL */")
    }
}

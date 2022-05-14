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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.StaticContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.transformers.Intrinsics
import org.jetbrains.kotlin.transformers.propertyAccessToFieldAccess
import org.jetbrains.kotlin.types.isDynamic

private val program = JsProgram()
private val staticCtx = StaticContext(program, Namer.newInstance(program.rootScope), program.rootScope)

val RECEIVER = "\$receiver"

private fun TODO(element: IrElement): Nothing = TODO(element.dump())

class JsBackendContext(val irBuiltIns: IrBuiltIns)

fun ir2js(module: IrModuleFragment): JsNode {
    extractBlockExpressions(module)
    applyIntrinsics(module)

    return module.accept(object : IrElementVisitor<JsNode, Data> {
        override fun visitElement(element: IrElement, data: Data): JsNode {
            TODO(element)
        }

        override fun visitModuleFragment(declaration: IrModuleFragment, data: Data): JsNode {
            // TODO
            val block = JsBlock()
            declaration.files.forEach { block.statements.add(it.accept(this, data) as JsStatement) }
            return block
        }

        override fun visitFile(declaration: IrFile, data: Data): JsNode {
            val block = JsBlock()
            val segments = declaration.packageFragmentDescriptor.fqName.pathSegments()

            val packageRef: JsExpression = if (segments.isNotEmpty()) {
                val name = segments.first().asString()
                block.statements.add(_var(program.scope.declareName(name), _or(JsNameRef(name), _object())))

                for (i in 1..segments.lastIndex) {
                    val part = segments.subList(1, i + 1).fold(JsNameRef(name)) { r, n -> JsNameRef(n.asString(), r) }
                    block.statements.add(_assignment(part, _or(part, _object())))
                }

                segments.subList(1, segments.size).fold(JsNameRef(name)) { r, n -> JsNameRef(n.asString(), r) }
            }
            else {
                JsThisRef()
            }

            // var foo = foo || {}
            // foo.bar = foo.bar || {}
            val generator = DeclarationGenerator(packageRef)

            for (d in declaration.declarations) {
                // TODO

                block.statements.add(d.accept(generator, data) as JsStatement)
            }
            return block
        }

    }, JsBackendContext(module.irBuiltins))
}

fun applyIntrinsics(module: IrModuleFragment) {
    val intrinsics = Intrinsics(module.irBuiltins)

    // TODO which order is better intrinsics than replace property accessors with field access or versa?
    // could we merge these passes?
    // see intrinsic for Array.size
    module.transformChildrenVoid(object: IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)

            val descriptor = expression.descriptor
            if (descriptor is CallableMemberDescriptor) {
                return intrinsics.get(descriptor)?.invoke(expression) ?: expression
            }

            return expression
        }
    })

    module.transformChildrenVoid(object: IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)
            return propertyAccessToFieldAccess(expression)
        }
    })
}

fun extractBlockExpressions(module: IrModuleFragment) {
    do {
        val extractor = ExpressionBlockExtractor(module.irBuiltins)
        module.accept(extractor, null)
    } while (extractor.changed)
}

fun IrBranch.isElse(): Boolean {
    val c = condition
    return c is IrConst<*> && c.value == true && c.startOffset == result.startOffset && c.endOffset == result.endOffset
}

typealias Data = JsBackendContext

interface BaseGenerator<out R : JsNode, in D> : IrElementVisitor<R, D> {
    override fun visitElement(element: IrElement, data: D): R {
        TODO(element.dump())
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: D): R {
        @Suppress("UNCHECKED_CAST")
        return JsEmpty as R
    }
}

private fun generateFunction(declaration: IrFunction, data: Data, packageRef: JsExpression? = null): JsStatement {
    val funName = declaration.descriptor.name.asString()
    val body = declaration.body?.accept(StatementGenerator(), data) as? JsBlock ?: JsBlock()
    val function = JsFunction(JsFunctionScope(program.scope, "scope for $funName"), body, "function $funName")

    fun JsFunction.addParameter(parameterName: String) {
        val parameter = function.scope.declareName(parameterName)
        parameters.add(JsParameter(parameter))
    }

    val descriptor = declaration.descriptor
    descriptor.valueParameters.forEach {
        function.addParameter(it.name.asString())
    }
    descriptor.extensionReceiverParameter?.let { function.addParameter(RECEIVER) }

    // TODO
    return if (packageRef != null) _assignment(_ref(declaration.descriptor.name.asString(), packageRef), function) else _var(program.scope.declareName(funName), function)
}

class FileGenerator : BaseGenerator<JsNode, Data>
class DeclarationGenerator(val packageRef: JsExpression) : BaseGenerator<JsNode, Data> {
    // TODO
    override fun visitDeclaration(declaration: IrDeclaration, data: Data) = JsEmpty

    override fun visitFunction(declaration: IrFunction, data: Data): JsNode {
        return generateFunction(declaration, data, packageRef)
    }

    override fun visitProperty(declaration: IrProperty, data: Data): JsNode {
        return _assignment(_ref(declaration.descriptor.name.asString(), packageRef), declaration.backingField?.initializer?.accept(ExpressionGenerator(), data) ?: _object()/*TODO Object.defineProperty?*/)
    }
}

class StatementGenerator : BaseGenerator<JsStatement, Data> {
//    fun visitElement(element: IrElement, data: D): R
//    fun visitModule(declaration: IrModule, data: D) = visitElement(declaration, data)
//    fun visitFile(declaration: IrFile, data: D) = visitElement(declaration, data)
//
//    fun visitDeclaration(declaration: IrDeclaration, data: D) = visitElement(declaration, data)
//    fun visitClass(declaration: IrClass, data: D) = visitDeclaration(declaration, data)
//    fun visitTypeAlias(declaration: IrTypeAlias, data: D) = visitDeclaration(declaration, data)
//    fun visitGeneralFunction(declaration: IrGeneralFunction, data: D) = visitDeclaration(declaration, data)
    override fun visitFunction(declaration: IrFunction, data: Data): JsStatement {
        return generateFunction(declaration, data)
    }
//    fun visitPropertyGetter(declaration: IrPropertyGetter, data: D) = visitGeneralFunction(declaration, data)
//    fun visitPropertySetter(declaration: IrPropertySetter, data: D) = visitGeneralFunction(declaration, data)
//    fun visitConstructor(declaration: IrConstructor, data: D) = visitGeneralFunction(declaration, data)
//    fun visitProperty(declaration: IrProperty, data: D) = visitDeclaration(declaration, data)
//    fun visitSimpleProperty(declaration: IrSimpleProperty, data: D) = visitProperty(declaration, data)
//    fun visitDelegatedProperty(declaration: IrDelegatedProperty, data: D) = visitProperty(declaration, data)
//    fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: D) = visitDeclaration(declaration, data)
//    fun visitLocalPropertyAccessor(declaration: IrLocalPropertyAccessor, data: D) = visitGeneralFunction(declaration, data)
    override fun visitVariable(declaration: IrVariable, data: Data): JsStatement {
        // TODO mark tmps
        return _var(program.scope.declareName(declaration.descriptor.name.asString()), declaration.initializer?.accept(ExpressionGenerator(), data))
    }
//    fun visitEnumEntry(declaration: IrEnumEntry, data: D) = visitDeclaration(declaration, data)
//    fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D) = visitDeclaration(declaration, data)
//
//    fun visitBody(body: IrBody, data: D) = visitElement(body, data)
//    fun visitExpressionBody(body: IrExpressionBody, data: D) = visitBody(body, data)
    override fun visitBlockBody(body: IrBlockBody, data: Data): JsStatement {
        return JsBlock(body.statements.map { it.accept(this, data) })
    }
//    fun visitSyntheticBody(body: IrSyntheticBody, data: D) = visitBody(body, data)
//
    override fun visitExpression(expression: IrExpression, data: Data): JsStatement {
        return JsExpressionStatement(expression.accept(ExpressionGenerator(), data))
    }


    override fun visitBlock(expression: IrBlock, data: Data): JsBlock {
        return JsBlock(expression.statements.map { it.accept(this, data) })
    }

    override fun visitComposite(expression: IrComposite, data: Data): JsStatement {
        // TODO introduce JsCompositeBlock?
        return JsBlock(expression.statements.map { it.accept(this, data) })
    }

//    fun visitStringConcatenation(expression: IrStringConcatenation, data: D) = visitExpression(expression, data)
//    fun visitThisReference(expression: IrThisReference, data: D) = visitExpression(expression, data)
//
//    fun visitDeclarationReference(expression: IrDeclarationReference, data: D) = visitExpression(expression, data)
//    fun visitSingletonReference(expression: IrGetSingletonValue, data: D) = visitDeclarationReference(expression, data)
//    fun visitGetObjectValue(expression: IrGetObjectValue, data: D) = visitSingletonReference(expression, data)
//    fun visitGetEnumValue(expression: IrGetEnumValue, data: D) = visitSingletonReference(expression, data)
//    fun visitVariableAccess(expression: IrVariableAccessExpression, data: D) = visitDeclarationReference(expression, data)
//    override fun visitGetVariable(expression: IrGetVariable, data: Data): JsExpression {
//    override fun visitSetVariable(expression: IrSetVariable, data: Data): JsExpression {
//    fun visitBackingFieldReference(expression: IrBackingFieldExpression, data: D) = visitDeclarationReference(expression, data)
//    fun visitGetBackingField(expression: IrGetBackingField, data: D) = visitBackingFieldReference(expression, data)
//    fun visitSetBackingField(expression: IrSetBackingField, data: D) = visitBackingFieldReference(expression, data)
//    fun visitGetExtensionReceiver(expression: IrGetExtensionReceiver, data: D) = visitDeclarationReference(expression, data)
//    fun visitGeneralCall(expression: IrGeneralCall, data: D) = visitDeclarationReference(expression, data)
//    override fun visitCall(expression: IrCall, data: Data): JsExpression {
//    fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: D) = visitGeneralCall(expression, data)
//    fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D) = visitGeneralCall(expression, data)
//    fun visitGetClass(expression: IrGetClass, data: D) = visitExpression(expression, data)
//
//    fun visitCallableReference(expression: IrCallableReference, data: D) = visitGeneralCall(expression, data)
//    fun visitClassReference(expression: IrClassReference, data: D) = visitDeclarationReference(expression, data)
//
//    fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: D) = visitExpression(expression, data)
//
//    fun visitTypeOperator(expression: IrTypeOperatorCall, data: D) = visitExpression(expression, data)

    override fun visitWhen(expression: IrWhen, data: Data): JsStatement {
        // TODO check empty when
        return expression.branches.foldRight<IrBranch, JsStatement?>(null) { br, n ->
            if (br.isElse()) br.result.accept(this, data)
            else JsIf(br.condition.accept(ExpressionGenerator(), data), br.result.accept(this, data), n)
        }!!
    }

//    fun visitLoop(loop: IrLoop, data: D) = visitExpression(loop, data)
    override fun visitWhileLoop(loop: IrWhileLoop, data: Data): JsStatement {
        //TODO what if body null?
        return JsWhile(loop.condition.accept(ExpressionGenerator(), data), loop.body?.accept(this, data))
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Data): JsStatement {
        //TODO what if body null?
        return JsDoWhile(loop.condition.accept(ExpressionGenerator(), data), loop.body?.accept(this, data))
    }


    override fun visitTry(aTry: IrTry, data: Data): JsStatement {
        val tryBlock = aTry.tryResult.accept(this, data)

        //TODO generate better name
        val catchBlock = JsCatch(program.scope, "e", aTry.catches.foldRight<IrCatch, JsStatement>(JsThrow(_ref("e"))) { l, r ->
            val catchParameter = l.parameter
            val catchBody = l.result.accept(this, data)
            catchParameter.type

            val refToType = staticCtx.getQualifiedReference(catchParameter.type.constructor.declarationDescriptor as ClassDescriptor)

            // TODO set condition
            JsIf(_instanceOf(_ref("e"), refToType), catchBody, r)
        })

        val finallyBlock = aTry.finallyExpression?.accept(this, data)

        return JsTry(/*TODO*/ tryBlock as JsBlock, catchBlock, /*TODO*/finallyBlock as JsBlock?)
    }


    override fun visitBreak(jump: IrBreak, data: Data): JsStatement {
        return JsBreak(jump.label?.let(::JsNameRef))
    }

    override fun visitContinue(jump: IrContinue, data: Data): JsStatement {
        return JsContinue(jump.label?.let(::JsNameRef))
    }


    override fun visitReturn(expression: IrReturn, data: Data): JsStatement {
        return expression.value.let { JsReturn(it.accept(ExpressionGenerator(), data)) }
    }
    override fun visitThrow(expression: IrThrow, data: Data): JsStatement {
        return JsThrow(expression.value.accept(ExpressionGenerator(), data))
    }
//
//    fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: D) = visitDeclaration(declaration, data)
//    fun visitErrorExpression(expression: IrErrorExpression, data: D) = visitExpression(expression, data)
//    fun visitErrorCallExpression(expression: IrErrorCallExpression, data: D) = visitErrorExpression(expression, data)

}

class ExpressionGenerator : BaseGenerator<JsExpression, Data> {
//    fun visitElement(element: IrElement, data: D): R
//    fun visitModule(declaration: IrModule, data: D) = visitElement(declaration, data)
//    fun visitFile(declaration: IrFile, data: D) = visitElement(declaration, data)
//
//    fun visitDeclaration(declaration: IrDeclaration, data: D) = visitElement(declaration, data)
//    fun visitClass(declaration: IrClass, data: D) = visitDeclaration(declaration, data)
//    fun visitTypeAlias(declaration: IrTypeAlias, data: D) = visitDeclaration(declaration, data)
//    fun visitGeneralFunction(declaration: IrGeneralFunction, data: D) = visitDeclaration(declaration, data)
//    fun visitFunction(declaration: IrFunction, data: D) = visitGeneralFunction(declaration, data)
//    fun visitPropertyGetter(declaration: IrPropertyGetter, data: D) = visitGeneralFunction(declaration, data)
//    fun visitPropertySetter(declaration: IrPropertySetter, data: D) = visitGeneralFunction(declaration, data)
//    fun visitConstructor(declaration: IrConstructor, data: D) = visitGeneralFunction(declaration, data)
//    fun visitProperty(declaration: IrProperty, data: D) = visitDeclaration(declaration, data)
//    fun visitSimpleProperty(declaration: IrSimpleProperty, data: D) = visitProperty(declaration, data)
//    fun visitDelegatedProperty(declaration: IrDelegatedProperty, data: D) = visitProperty(declaration, data)
//    fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: D) = visitDeclaration(declaration, data)
//    fun visitLocalPropertyAccessor(declaration: IrLocalPropertyAccessor, data: D) = visitGeneralFunction(declaration, data)
//    fun visitVariable(declaration: IrVariable, data: D) = visitDeclaration(declaration, data)
//    fun visitEnumEntry(declaration: IrEnumEntry, data: D) = visitDeclaration(declaration, data)
//    fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D) = visitDeclaration(declaration, data)
//
//    fun visitBody(body: IrBody, data: D) = visitElement(body, data)
    override fun visitExpressionBody(body: IrExpressionBody, data: Data): JsExpression {
        return body.expression.accept(this, data)
    }
//    fun visitBlockBody(body: IrBlockBody, data: D) = visitBody(body, data)
//    fun visitSyntheticBody(body: IrSyntheticBody, data: D) = visitBody(body, data)
//
//    fun visitExpression(expression: IrExpression, data: D) = visitElement(expression, data)
    override fun <T> visitConst(expression: IrConst<T>, data: Data): JsExpression {
    val kind = expression.kind
    return when (kind) {
            is IrConstKind.String -> JsStringLiteral(kind.valueOf(expression))
            is IrConstKind.Null -> JsNullLiteral()
            is IrConstKind.Boolean -> if (kind.valueOf(expression)) JsBooleanLiteral(true) else JsBooleanLiteral(false)
            is IrConstKind.Char -> JsIntLiteral(kind.valueOf(expression).toInt()) // TODO
            is IrConstKind.Byte -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> JsIntLiteral(kind.valueOf(expression))
            is IrConstKind.Long -> JsDoubleLiteral(kind.valueOf(expression).toDouble()) // TODO
            is IrConstKind.Float -> JsDoubleLiteral(kind.valueOf(expression).toDouble())
            is IrConstKind.Double -> JsDoubleLiteral(kind.valueOf(expression))
        }
    }
    override fun visitVararg(expression: IrVararg, data: Data): JsExpression {
        //TODO native
        val hasSpread = expression.elements.any { it is IrSpreadElement }
        val elements = expression.elements.map { it.accept(this, data) }
        // TODO use a.slice() when it possible
        return if (!hasSpread) JsArrayLiteral(elements) else JsInvocation(JsNameRef("concat", JsArrayLiteral()), elements)
    }
    override fun visitSpreadElement(spread: IrSpreadElement, data: Data): JsExpression {
        return spread.expression.accept(this, data)
    }

    override fun visitBlock(expression: IrBlock, data: Data): JsExpression {
        // TODO empty?
        val statements = expression.statements
        return statements.subList(1, statements.size).fold(statements[0].accept(this, data)) { r, st ->
            JsBinaryOperation(JsBinaryOperator.COMMA, r, statements[0].accept(this, data))
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Data): JsExpression {
        // TODO revisit
        return expression.arguments.fold<IrExpression, JsExpression>(JsStringLiteral("")) { jsExpr, irExpr -> _plus(jsExpr, irExpr.accept(this, data)) }
    }
//    fun visitThisReference(expression: IrThisReference, data: D) = visitExpression(expression, data)
//
//    fun visitDeclarationReference(expression: IrDeclarationReference, data: D) = visitExpression(expression, data)
//    fun visitSingletonReference(expression: IrGetSingletonValue, data: D) = visitDeclarationReference(expression, data)
    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Data): JsExpression {
        // TODO implement
        return JsNameRef("TODO")
    }
    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Data): JsExpression {
        // TODO implement
        return JsNameRef("TODO")
    }

    override fun visitGetValue(expression: IrGetValue, data: Data): JsExpression {
        // TODO support `this` and receiver
        return JsNameRef(expression.descriptor.name.asString())
    }

    override fun visitSetVariable(expression: IrSetVariable, data: Data): JsExpression {
        val v = JsNameRef(expression.descriptor.name.asString())
        val value = expression.value.accept(this, data)
        return JsBinaryOperation(JsBinaryOperator.ASG, v, value)
    }

    override fun visitGetField(expression: IrGetField, data: Data): JsExpression {
        return _ref(expression.descriptor.name.asString(), expression.receiver?.accept(this, data))
    }

    override fun visitSetField(expression: IrSetField, data: Data): JsExpression {
        return _assignment(_ref(expression.descriptor.name.asString(), expression.receiver?.accept(this, data)), expression.value.accept(this, data)).expression
    }

//    fun visitGeneralCall(expression: IrGeneralCall, data: D) = visitDeclarationReference(expression, data)
    override fun visitCall(expression: IrCall, data: Data): JsExpression {
//        (expression.descriptor as? CallableMemberDescriptor)?.let {
//            val t = Intrinsics(data.irBuiltIns).getJS(it)?.invoke(expression)
//        }

        val (op_, arg1) = when(expression.descriptor) {
            data.irBuiltIns.eqeqeq -> JsBinaryOperator.REF_EQ to expression.getValueArgument(1)
            data.irBuiltIns.eqeq -> JsBinaryOperator.EQ to expression.getValueArgument(1)
            data.irBuiltIns.lt0 -> JsBinaryOperator.LT to IrConstImpl.int(expression.startOffset, expression.endOffset, expression.type, 0)
            data.irBuiltIns.lteq0 -> JsBinaryOperator.LTE to IrConstImpl.int(expression.startOffset, expression.endOffset, expression.type, 0)
            data.irBuiltIns.gt0 -> JsBinaryOperator.GT to IrConstImpl.int(expression.startOffset, expression.endOffset, expression.type, 0)
            data.irBuiltIns.gteq0 -> JsBinaryOperator.GTE to IrConstImpl.int(expression.startOffset, expression.endOffset, expression.type, 0)
            data.irBuiltIns.throwNpe -> return throwNPE()
            data.irBuiltIns.booleanNot -> return JsPrefixOperation(JsUnaryOperator.NOT, expression.getValueArgument(0)!!.accept(this, data))
            else -> null to null
        }

        if (op_ != null && arg1 != null)
            return JsBinaryOperation(op_, expression.getValueArgument(0)!!.accept(this, data), arg1.accept(this, data))

        when (expression) {
            is IrBinaryPrimitiveImpl -> {
                val op = when (expression.origin) {
                    IrStatementOrigin.PLUS -> JsBinaryOperator.ADD
                    IrStatementOrigin.MINUS -> JsBinaryOperator.SUB
                    IrStatementOrigin.MUL -> JsBinaryOperator.MUL
                    IrStatementOrigin.DIV -> JsBinaryOperator.DIV
                    IrStatementOrigin.EQEQ -> JsBinaryOperator.EQ
                    IrStatementOrigin.EQEQEQ -> JsBinaryOperator.REF_EQ
                    IrStatementOrigin.EXCLEQ -> JsBinaryOperator.NEQ
                    IrStatementOrigin.EXCLEQEQ -> JsBinaryOperator.REF_NEQ
                // TODO map all
                    else -> null
                }
                if (op != null) return JsBinaryOperation(op, expression.argument0.accept(this, data), expression.argument1.accept(this, data))
            }
//            is IrUnaryPrimitiveImpl ->
        }

        val descriptor = expression.descriptor
        //  JS-code
        if (descriptor is SimpleFunctionDescriptor && JsCallChecker.JS_PATTERN.test(descriptor)) {
            // TODO it can be non-expression
            val jsCode = translateJsCode(expression, program.scope)
            if (jsCode is JsExpression) return jsCode
            return JsInvocation(JsFunction(program.scope, JsBlock(jsCode as JsStatement), ""))
        }

        val dispatchReceiver = expression.dispatchReceiver?.accept(this, data)
        val extensionReceiver = expression.extensionReceiver?.accept(this, data)
        val d = dispatchReceiver ?: run {
            val segments = descriptor.fqNameUnsafe.pathSegments()
            // TODO ISSUE
            segments.joinToString { it.asString() }
//            segments.subList(0, segments.lastIndex).fold()
        }
        val ref = if (dispatchReceiver != null) JsNameRef(descriptor.name.asString().sanitize(), dispatchReceiver) else JsNameRef(descriptor.fqNameUnsafe.pathSegments().joinToString(".") { it.asString() }.sanitize())
        var latestProvidedArgumentIndex = 0
        val arguments =
                // TODO mapTo
                descriptor.valueParameters.map {
                    val argument = expression.getValueArgument(it.index)
                    if (argument != null) {
                        latestProvidedArgumentIndex = it.index
                        argument.accept(this, data)
                    }
                    else {
                        JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
                    }
                }.take(latestProvidedArgumentIndex + 1)


        //TODO
        if (descriptor is ConstructorDescriptor && descriptor.isPrimary) {
            return JsNew(staticCtx.getQualifiedReference(descriptor), arguments)
        }

        return JsInvocation(ref, extensionReceiver?.let { listOf(extensionReceiver) + arguments } ?: arguments)
    }
//    fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: D) = visitGeneralCall(expression, data)
//    fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D) = visitGeneralCall(expression, data)
//    fun visitGetClass(expression: IrGetClass, data: D) = visitExpression(expression, data)
//
//    fun visitCallableReference(expression: IrCallableReference, data: D) = visitGeneralCall(expression, data)
//    fun visitClassReference(expression: IrClassReference, data: D) = visitDeclarationReference(expression, data)
//
//    fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: D) = visitExpression(expression, data)
//
   override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Data): JsExpression {
        // TODO better name
        val argument = expression.argument.accept(this, data)
        //  TODO fix
        val type = JsNameRef(expression.typeOperand.constructor.declarationDescriptor!!.name.asString())

        // kotlin.isType ?
        // TODO review
        return when(expression.operator) {
            IrTypeOperator.CAST -> JsConditional(_instanceOf(argument, type), argument, throwCCE())
            IrTypeOperator.IMPLICIT_CAST -> argument // JsConditional(_instanceOf(argument, type), argument, throwCCE()) // TODO what should we do in JS here?
            IrTypeOperator.IMPLICIT_NOTNULL -> if (expression.argument.type.isDynamic()) argument else JsConditional(_identityEquals(argument, JsNullLiteral()), argument, throwCCE()) // TODO what should we do in JS here?
            IrTypeOperator.SAFE_CAST -> JsConditional(_instanceOf(argument, type), argument, JsNullLiteral())
            IrTypeOperator.INSTANCEOF -> _instanceOf(argument, type)
            IrTypeOperator.NOT_INSTANCEOF -> _not(_instanceOf(argument, type))
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> argument // TODO: ?
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> TODO()
        }
   }


    override fun visitWhen(expression: IrWhen, data: Data): JsExpression {
        // TODO check empty when
        return expression.branches.foldRight<IrBranch, JsExpression?>(null) { br, n ->
            if (br.isElse()) br.result.accept(this, data)
            else JsConditional(br.condition.accept(this, data), br.result.accept(this, data), n)
        }!!
    }
}

// TODO: remove
private fun String.sanitize() = this.replace("[-<>\\s]".toRegex(), "_")

/*
rewrite on ir:
* when expressions
* try-catch
* many catch blocks -> one catch with many ifs
* return, break, continue
* block expressions
*
* class

*/

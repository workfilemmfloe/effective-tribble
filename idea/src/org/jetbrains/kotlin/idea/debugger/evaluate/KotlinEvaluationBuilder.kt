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

package org.jetbrains.kotlin.idea.debugger.evaluate

import org.jetbrains.kotlin.idea.debugger.evaluate.*
import com.intellij.psi.PsiElement
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.*
import com.intellij.debugger.engine.evaluation.expression.*
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.PsiFileFactory
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.Opcodes.ASM5
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.jdi.JDIEval
import org.jetbrains.eval4j.jdi.asJdiValue
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import com.sun.jdi.request.EventRequest
import com.sun.jdi.ObjectReference
import com.intellij.debugger.engine.SuspendContext
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinEvaluateExpressionCache.*
import org.jetbrains.kotlin.resolve.BindingContext
import com.sun.jdi.VirtualMachine
import org.jetbrains.kotlin.codegen.AsmUtil
import com.sun.jdi.InvalidStackFrameException
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.analysisContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.caches.resolve.JavaResolveExtension
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.types.Flexibility
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.idea.util.attachment.attachmentByPsiFile
import com.intellij.openapi.diagnostic.Attachment
import org.jetbrains.kotlin.idea.util.attachment.mergeAttachments
import com.sun.jdi.ClassType
import com.sun.jdi.InvocationException

private val RECEIVER_NAME = "\$receiver"
private val THIS_NAME = "this"

object KotlinEvaluationBuilder: EvaluatorBuilder {
    override fun build(codeFragment: PsiElement, position: SourcePosition?): ExpressionEvaluator {
        if (codeFragment !is JetCodeFragment || position == null) {
            return EvaluatorBuilderImpl.getInstance()!!.build(codeFragment, position)
        }

        val file = position.getFile()
        if (file !is JetFile) {
            throw EvaluateExceptionUtil.createEvaluateException("Couldn't evaluate kotlin expression in non-kotlin context")
        }

        if (codeFragment.getContext() !is JetElement) {
            val attachments = array(attachmentByPsiFile(position.getFile()),
                                    attachmentByPsiFile(codeFragment),
                                    Attachment("breakpoint.info", "line: ${position.getLine()}"))

            logger.error("Trying to evaluate ${codeFragment.javaClass} with context ${codeFragment.getContext()?.javaClass}", mergeAttachments(*attachments))
            throw EvaluateExceptionUtil.createEvaluateException("Couldn't evaluate kotlin expression in this context")
        }

        val packageName = file.getPackageDirective()?.getFqName()?.asString()
        if (packageName != null && packageName.isNotEmpty()) {
            codeFragment.addImportsFromString("import $packageName.*")
        }
        return ExpressionEvaluatorImpl(KotlinEvaluator(codeFragment as JetCodeFragment, position))
    }
}

val logger = Logger.getInstance(javaClass<KotlinEvaluator>())

class KotlinEvaluator(val codeFragment: JetCodeFragment,
                      val sourcePosition: SourcePosition
) : Evaluator {
    override fun evaluate(context: EvaluationContextImpl): Any? {
        var isCompiledDataFromCache = true
        try {
            val compiledData = KotlinEvaluateExpressionCache.getOrCreateCompiledData(codeFragment, sourcePosition, context) {
                fragment, position ->
                isCompiledDataFromCache = false
                extractAndCompile(fragment, position)
            }
            val result = runEval4j(context, compiledData)

            val virtualMachine = context.getDebugProcess().getVirtualMachineProxy().getVirtualMachine()

            // If bytecode was taken from cache and exception was thrown - recompile bytecode and run eval4j again
            if (isCompiledDataFromCache && result is ExceptionThrown && result.kind == ExceptionThrown.ExceptionKind.BROKEN_CODE) {
                return runEval4j(context, extractAndCompile(codeFragment, sourcePosition)).toJdiValue(virtualMachine)
            }

            return result.toJdiValue(virtualMachine)
        }
        catch(e: EvaluateException) {
            throw e
        }
        catch (e: Exception) {
            logger.error("Couldn't evaluate expression:\n" +
                         "FILE NAME: ${sourcePosition.getFile().getName()}\n" +
                         "BREAKPOINT LINE: ${sourcePosition.getLine()}\n" +
                         "CODE FRAGMENT:\n${codeFragment.getText()}\n" +
                         "FILE TEXT: \n${sourcePosition.getFile().getText()}\n", e)

            val cause = if (e.getMessage() != null) ": ${e.getMessage()}" else ""
            exception("An exception occurs during Evaluate Expression Action $cause")
        }
    }

    override fun getModifier(): Modifier? {
        return null
    }

    class object {
        private fun extractAndCompile(codeFragment: JetCodeFragment, sourcePosition: SourcePosition): CompiledDataDescriptor {
            codeFragment.checkForErrors()

            val extractedFunction = getFunctionForExtractedFragment(codeFragment, sourcePosition.getFile(), sourcePosition.getLine())
            if (extractedFunction == null) {
                throw IllegalStateException("Code fragment cannot be extracted to function")
            }

            val classFileFactory = createClassFileFactory(codeFragment, extractedFunction)

            // KT-4509
            val outputFiles = (classFileFactory : OutputFileCollection).asList().filter { it.relativePath != "$packageInternalName.class" }
            if (outputFiles.size() != 1) exception("Expression compiles to more than one class file. Note that lambdas, classes and objects are unsupported yet. List of files: ${outputFiles.joinToString(",")}")

            val funName = extractedFunction.getName()
            if (funName == null) {
                throw IllegalStateException("Extracted function should have a name: ${extractedFunction.getText()}")
            }
            return CompiledDataDescriptor(outputFiles.first().asByteArray(), sourcePosition, funName, extractedFunction.getParametersForDebugger())
        }

        private fun runEval4j(context: EvaluationContextImpl, compiledData: CompiledDataDescriptor): InterpreterResult {
            val virtualMachine = context.getDebugProcess().getVirtualMachineProxy().getVirtualMachine()

            var resultValue: InterpreterResult? = null
            ClassReader(compiledData.bytecodes).accept(object : ClassVisitor(ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    if (name == compiledData.funName) {
                        val args = context.getArgumentsForEval4j(compiledData.parameters.getParameterNames(), Type.getArgumentTypes(desc))
                        return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                            override fun visitEnd() {
                                val breakpoints = virtualMachine.eventRequestManager().breakpointRequests()
                                breakpoints?.forEach { it.disable() }

                                resultValue = interpreterLoop(
                                        this,
                                        makeInitialFrame(this, args),
                                        JDIEval(virtualMachine,
                                                context.getClassLoader()!!,
                                                context.getSuspendContext().getThread()?.getThreadReference()!!,
                                                context.getSuspendContext().getInvokePolicy())
                                )

                                breakpoints?.forEach { it.enable() }
                            }
                        }
                    }

                    return super.visitMethod(access, name, desc, signature, exceptions)
                }
            }, 0)

            return resultValue ?: throw IllegalStateException("resultValue is null: cannot find method ${compiledData.funName}")
        }

        private fun InterpreterResult.toJdiValue(vm: VirtualMachine): com.sun.jdi.Value? {
            val jdiValue = when (this) {
                is ValueReturned -> result
                is ExceptionThrown -> {
                    if (this.kind == ExceptionThrown.ExceptionKind.FROM_EVALUATED_CODE) {
                        exception(InvocationException(this.exception.value as ObjectReference))
                    }
                    else {
                        exception(exception.toString())
                    }
                }
                is AbnormalTermination -> exception(message)
                else -> throw IllegalStateException("Unknown result value produced by eval4j")
            }
            return jdiValue.asJdiValue(vm, jdiValue.asmType)
        }

        private fun JetNamedFunction.getParametersForDebugger(): ParametersDescriptor {
            return runReadAction {
                val parameters = ParametersDescriptor()
                val bindingContext = analyzeFullyAndGetResult().bindingContext
                val descriptor = bindingContext[BindingContext.FUNCTION, this]
                if (descriptor != null) {
                    val receiver = descriptor.getExtensionReceiverParameter()
                    if (receiver != null) {
                        parameters.add(THIS_NAME, receiver.getType())
                    }

                    descriptor.getValueParameters().forEach {
                        param ->
                        parameters.add(param.getName().asString(), param.getType())
                    }
                }
                parameters
            }
        }

        private fun EvaluationContextImpl.getArgumentsForEval4j(parameterNames: List<String>, parameterTypes: Array<Type>): List<Value> {
            return parameterNames.zip(parameterTypes).map { this.findLocalVariable(it.first, it.second, checkType = false, failIfNotFound = true)!! }
        }

        private fun createClassFileFactory(codeFragment: JetCodeFragment, extractedFunction: JetNamedFunction): ClassFileFactory {
            return runReadAction {
                val file = createFileForDebugger(codeFragment, extractedFunction)

                val (bindingContext, moduleDescriptor) = file.checkForErrors()

                val state = GenerationState(
                        file.getProject(),
                        ClassBuilderFactories.BINARIES,
                        moduleDescriptor,
                        bindingContext,
                        listOf(file)
                )

                KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION)

                state.getFactory()
            }
        }

        private fun exception(msg: String) = throw EvaluateExceptionUtil.createEvaluateException(msg)

        private fun exception(e: Throwable): Nothing {
            val message = e.getMessage()
            if (message != null) {
                throw EvaluateExceptionUtil.createEvaluateException(message, e)
            }
            throw EvaluateExceptionUtil.createEvaluateException(e)
        }

        private fun JetFile.checkForErrors() =
            runReadAction {
                try {
                    AnalyzingUtils.checkForSyntacticErrors(this)
                }
                catch (e: IllegalArgumentException) {
                    throw EvaluateExceptionUtil.createEvaluateException(e.getMessage())
                }

                val analysisResult = this.analyzeFullyAndGetResult(createFlexibleTypesFile())
                if (analysisResult.isError()) {
                    throw EvaluateExceptionUtil.createEvaluateException(analysisResult.error)
                }

                val bindingContext = analysisResult.bindingContext
                bindingContext.getDiagnostics().firstOrNull { it.getSeverity() == Severity.ERROR }?.let {
                    throw EvaluateExceptionUtil.createEvaluateException(DefaultErrorMessages.render(it))
                }

                analysisResult
            }
    }
}

private val template = """
package packageForDebugger

!IMPORT_LIST!

!FUNCTION!
"""

private val packageInternalName = PackageClassUtils.getPackageClassInternalName(FqName("packageForDebugger"))

private fun createFileForDebugger(codeFragment: JetCodeFragment,
                                  extractedFunction: JetNamedFunction
): JetFile {
    var fileText = template.replace("!IMPORT_LIST!",
                                    codeFragment.importsToString()
                                            .split(JetCodeFragment.IMPORT_SEPARATOR)
                                            .joinToString("\n"))

    val extractedFunctionText = extractedFunction.getText()
    assert(extractedFunctionText != null, "Text of extracted function shouldn't be null")
    fileText = fileText.replace("!FUNCTION!", extractedFunction.getText()!!)

    val jetFile = codeFragment.createJetFile("debugFile.kt", fileText)
    jetFile.suppressDiagnosticsInDebugMode = true
    return jetFile
}

private fun PsiElement.createFlexibleTypesFile(): JetFile {
    return createJetFile(
            "FLEXIBLE_TYPES.kt",
            """
                package ${Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getPackageFqName()}
                public class ${Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getRelativeClassName()}<L, U>
            """
    )
}

private fun PsiElement.createJetFile(fileName: String, fileText: String): JetFile {
    // Not using JetPsiFactory because we need a virtual file attached to the JetFile
    val virtualFile = LightVirtualFile(fileName, JetLanguage.INSTANCE, fileText)
    virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
    val jetFile = (PsiFileFactory.getInstance(getProject()) as PsiFileFactoryImpl)
            .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false) as JetFile
    jetFile.analysisContext = this
    return jetFile
}

private fun SuspendContext.getInvokePolicy(): Int {
    return if (getSuspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD) ObjectReference.INVOKE_SINGLE_THREADED else 0
}

private fun com.sun.jdi.Type?.isSubclass(superClassName: String): Boolean {
    if (this !is ClassType) return false
    if (allInterfaces().any { it.name() == superClassName }) {
        return true
    }

    var superClass = this.superclass()
    while (superClass != null) {
        if (superClass.name() == superClassName) {
            return true
        }
        superClass = superClass.superclass()
    }
    return false
}

fun EvaluationContextImpl.findLocalVariable(name: String, asmType: Type?, checkType: Boolean, failIfNotFound: Boolean): Value? {
    val project = getDebugProcess().getProject()
    val frame = getFrameProxy()?.getStackFrame()
    if (frame == null) return null

    fun isValueOfCorrectType(value: Value, asmType: Type?, shouldCheckType: Boolean): Boolean {
        if (!shouldCheckType || asmType == null || value.asmType == asmType) return true
        if (project == null) return false

        if ((value.obj() as? com.sun.jdi.ObjectReference)?.referenceType().isSubclass(asmType.getClassName())) {
            return true
        }

        val thisDesc = value.asmType.getClassDescriptor(project)
        val expDesc = asmType.getClassDescriptor(project)
        return thisDesc != null && expDesc != null && runReadAction { DescriptorUtils.isSubclass(thisDesc, expDesc) }
    }


    try {
        when (name) {
            THIS_NAME -> {
                val thisObject = frame.thisObject()
                if (thisObject != null) {
                    val eval4jValue = thisObject.asValue()
                    if (isValueOfCorrectType(eval4jValue, asmType, true)) return eval4jValue
                }

                val receiver = findLocalVariable(RECEIVER_NAME, asmType, checkType = true, failIfNotFound = false)
                if (receiver != null) return receiver

                val this0 = findLocalVariable(AsmUtil.CAPTURED_THIS_FIELD, asmType, checkType = true, failIfNotFound = false)
                if (this0 != null) return this0

                val `$this` = findLocalVariable("\$this", asmType, checkType = false, failIfNotFound = false)
                if (`$this` != null) return `$this`
            }
            else -> {
                val eval4j = JDIEval(frame.virtualMachine()!!,
                                     getClassLoader()!!,
                                     getSuspendContext().getThread()?.getThreadReference()!!,
                                     getSuspendContext().getInvokePolicy())

                fun JDIEval.getField(owner: Value, name: String, asmType: Type?, checkType: Boolean): Value? {
                    val fieldDescription = FieldDescription(owner.asmType.getInternalName(), name, asmType?.getDescriptor() ?: "", isStatic = false)
                    try {
                        val fieldValue = getField(owner, fieldDescription)
                        if (isValueOfCorrectType(fieldValue, asmType, checkType)) return fieldValue
                        return null
                    }
                    catch (e: Exception) {
                        return null
                    }
                }

                fun Value.isSharedVar(expectedType: Type?): Boolean  {
                    return expectedType != null && this.asmType == StackValue.sharedTypeForType(expectedType)
                }

                fun JDIEval.getValueForSharedVar(value: Value, expectedType: Type?, checkType: Boolean): Value? {
                    val sharedVarValue = this.getField(value, "element", expectedType, checkType)
                    if (sharedVarValue != null && isValueOfCorrectType(sharedVarValue, expectedType, checkType)) {
                        return sharedVarValue
                    }
                    return null
                }

                val localVariable = frame.visibleVariableByName(name)
                if (localVariable != null) {
                    val eval4jValue = frame.getValue(localVariable).asValue()
                    if (eval4jValue.isSharedVar(asmType)) {
                        val sharedVarValue = eval4j.getValueForSharedVar(eval4jValue, asmType, checkType)
                        if (sharedVarValue != null) {
                            return sharedVarValue
                        }
                    }

                    if (isValueOfCorrectType(eval4jValue, asmType, checkType)) return eval4jValue
                }

                fun findCapturedVal(name: String): Value? {
                    var result: Value? = null
                    val thisObject = frame.thisObject() ?: return null
                    var thisObj: Value? = thisObject.asValue()

                    while (result == null && thisObj != null) {
                        result = eval4j.getField(thisObj!!, name, asmType, checkType)
                        if (result == null) {
                            thisObj = eval4j.getField(thisObj!!, AsmUtil.CAPTURED_THIS_FIELD, null, false)
                        }
                    }
                    return result
                }

                val capturedValName = getCapturedFieldName(name)
                val capturedVal = findCapturedVal(capturedValName)
                if (capturedVal != null) {
                    if (capturedVal.isSharedVar(asmType)) {
                        val sharedVarValue = eval4j.getValueForSharedVar(capturedVal, asmType, checkType)
                        if (sharedVarValue != null) {
                            return sharedVarValue
                        }
                    }
                    return capturedVal
                }
            }
        }

        return if (!failIfNotFound)
            null
        else
            throw EvaluateExceptionUtil.createEvaluateException("Cannot find local variable: name = $name${if (checkType) ", type = " + asmType.toString() else ""}")
    }
    catch(e: InvalidStackFrameException) {
        throw EvaluateExceptionUtil.createEvaluateException("Local variable $name is unavailable in current frame")
    }
}

fun Type.getClassDescriptor(project: Project): ClassDescriptor? {
    if (AsmUtil.isPrimitive(this)) return null

    val jvmName = JvmClassName.byInternalName(getInternalName()).getFqNameForClassNameWithoutDollars()

    val platformClasses = JavaToKotlinClassMap.INSTANCE.mapPlatformClass(jvmName)
    if (platformClasses.notEmpty) return platformClasses.first()

    return runReadAction {
        val classes = JavaPsiFacade.getInstance(project).findClasses(jvmName.asString(), GlobalSearchScope.allScope(project))
        if (classes.isEmpty()) null
        else {
            val clazz = classes.first()
            JavaResolveExtension.getResolver(project, clazz).resolveClass(JavaClassImpl(clazz))
        }
    }
}

private fun getCapturedFieldName(name: String) = when (name) {
    RECEIVER_NAME -> AsmUtil.CAPTURED_RECEIVER_FIELD
    THIS_NAME -> AsmUtil.CAPTURED_THIS_FIELD
    AsmUtil.CAPTURED_RECEIVER_FIELD -> name
    AsmUtil.CAPTURED_THIS_FIELD -> name
    else -> "$$name"
}


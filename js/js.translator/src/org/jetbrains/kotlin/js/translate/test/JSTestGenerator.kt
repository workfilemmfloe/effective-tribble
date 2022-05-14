/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.test

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

//TODO: use method object instead of static functions
class JSTestGenerator(val context: TranslationContext) {
    private fun findFunction(name: String): JsExpression {
        for (d in context.config.moduleDescriptors) {
            if ("<kotlin-test>" == d.name) {
                val descriptor = DescriptorUtils.getFunctionByNameOrNull(d.data.getPackage(FqName.ROOT).memberScope,
                                                                         Name.identifier(name)) ?: continue
                return context.getQualifiedReference(descriptor)
            }
        }
        return JsNameRef(name, JsNameRef("Kotlin"))
    }

    private val suiteRef: JsExpression = findFunction("suite")
    private val testRef: JsExpression = findFunction("test")
    private val ignoreRef: JsExpression = findFunction("ignore")

    fun generateTestCalls(moduleDescriptor: ModuleDescriptor) = generateTestCalls(moduleDescriptor, FqName.ROOT)

    private fun generateTestCalls(moduleDescriptor: ModuleDescriptor, packageName: FqName) {
        for (packageDescriptor in moduleDescriptor.getPackage(packageName).fragments) {
            if (DescriptorUtils.getContainingModule(packageDescriptor) !== moduleDescriptor) continue

            packageDescriptor.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.ALL_NAME_FILTER).forEach {
                if (it is ClassDescriptor) {
                    generateTestFunctions(it)
                }
            }
        }

        for (subpackageName in moduleDescriptor.getSubPackagesOf(packageName, MemberScope.ALL_NAME_FILTER)) {
            generateTestCalls(moduleDescriptor, subpackageName)
        }
    }

    private fun generateTestFunctions(classDescriptor: ClassDescriptor) {
        if (classDescriptor.modality === Modality.ABSTRACT) return

        val suiteFunction = JsFunction(context.scope(), JsBlock(), "suite function")

        classDescriptor.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, MemberScope.ALL_NAME_FILTER).forEach {
            if (it is FunctionDescriptor && isTest(it)) {
                generateCodeForTestMethod(it, classDescriptor, suiteFunction)
            }
        }

        if (!suiteFunction.body.isEmpty) {
            val suiteName = context.program().getStringLiteral(classDescriptor.name.toString())
            context.addTopLevelStatement(JsInvocation(suiteRef, suiteName, suiteFunction).makeStmt())
        }
    }

    private fun generateCodeForTestMethod(functionDescriptor: FunctionDescriptor, classDescriptor: ClassDescriptor, parentFun: JsFunction) {
        val functionToTest = generateTestFunction(functionDescriptor, classDescriptor, parentFun.scope)

        val ref = if (isIgnore(functionDescriptor)) ignoreRef else testRef

        val testName = context.program().getStringLiteral(functionDescriptor.name.toString())
        parentFun.body.statements += JsInvocation(ref, testName, functionToTest).makeStmt()
    }

    private fun generateTestFunction(functionDescriptor: FunctionDescriptor, classDescriptor: ClassDescriptor, scope: JsScope): JsFunction {
        val expression = ReferenceTranslator.translateAsValueReference(classDescriptor, context)
        val testClass = JsNew(expression)
        val functionToTestCall = CallTranslator.buildCall(context, functionDescriptor, emptyList<JsExpression>(), testClass)
        val functionToTest = JsFunction(scope, "test function")
        functionToTest.body = JsBlock(functionToTestCall.makeStmt())

        return functionToTest
    }

    /**
     * JUnit3 style:
     * if (function.getName().startsWith("test")) {
     *   List<JetParameter> parameters = function.getValueParameters();
     *   return parameters.size() == 0;
     * }
     */
    private fun isTest(functionDescriptor: FunctionDescriptor)
            = functionDescriptor.annotations.any(annotationFinder("Test", "kotlin.test.Test"))

    private fun isIgnore(functionDescriptor: FunctionDescriptor)
            = functionDescriptor.annotations.any(annotationFinder("Ignore", "kotlin.test.Ignore"))

    private fun annotationFinder(shortName: String, fqName: String) = { annotation: AnnotationDescriptor ->
        annotation.type.toString() == shortName && run {
            val descriptor = annotation.type.constructor.declarationDescriptor
            descriptor != null && FqNameUnsafe(fqName) == DescriptorUtils.getFqName(descriptor)
        }
    }
}

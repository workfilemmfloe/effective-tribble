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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParenthesizerOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import java.util.*

public object KotlinNameSuggester {
    public fun suggestNamesByExpressionAndType(
            expression: KtExpression,
            type: KotlinType?,
            bindingContext: BindingContext?,
            validator: (String) -> Boolean,
            defaultName: String?
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        (type ?: bindingContext?.getType(expression))?.let {
            result.addNamesByType(it, validator)
        }

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    public fun suggestNamesByType(type: KotlinType, validator: (String) -> Boolean, defaultName: String? = null): List<String> {
        val result = ArrayList<String>()

        result.addNamesByType(type, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    public fun suggestNamesByExpressionOnly(
            expression: KtExpression,
            bindingContext: BindingContext?,
            validator: (String) -> Boolean, defaultName: String? = null): List<String> {
        val result = ArrayList<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    public fun suggestIterationVariableNames(
            collection: KtExpression,
            elementType: KotlinType,
            bindingContext: BindingContext?,
            validator: (String) -> Boolean, defaultName: String?): Collection<String> {
        val result = LinkedHashSet<String>()

        suggestNamesByExpressionOnly(collection, bindingContext, { true })
                .mapNotNull { StringUtil.unpluralize(it) }
                .mapTo(result) { suggestNameByName(it, validator) }

        result.addNamesByType(elementType, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    private val COMMON_TYPE_PARAMETER_NAMES = listOf("T", "U", "V", "W", "X", "Y", "Z")

    public fun suggestNamesForTypeParameters(count: Int, validator: (String) -> Boolean): List<String> {
        val result = ArrayList<String>()
        for (i in 0..count - 1) {
            result.add(suggestNameByMultipleNames(COMMON_TYPE_PARAMETER_NAMES, validator))
        }
        return result
    }

    /**
     * Validates name, and slightly improves it by adding number to name in case of conflicts
     * @param name to check it in scope
     * @return name or nameI, where I is number
     */
    public fun suggestNameByName(name: String, validator: (String) -> Boolean): String {
        if (validator(name)) return name
        var i = 1
        while (!validator(name + i)) {
            ++i
        }

        return name + i
    }

    /**
     * Validates name using set of variants which are tried in succession (and extended with suffixes if necessary)
     * For example, when given sequence of a, b, c possible names are tried out in the following order: a, b, c, a1, b1, c1, a2, b2, c2, ...
     * @param names to check it in scope
     * @return name or nameI, where name is one of variants and I is a number
     */
    public fun suggestNameByMultipleNames(names: Collection<String>, validator: (String) -> Boolean): String {
        var i = 0
        while (true) {
            for (name in names) {
                val candidate = if (i > 0) name + i else name
                if (validator(candidate)) return candidate
            }
            i++
        }
    }

    private fun MutableCollection<String>.addNamesByType(type: KotlinType, validator: (String) -> Boolean) {
        var type = TypeUtils.makeNotNullable(type) // wipe out '?'
        val builtIns = type.builtIns
        val typeChecker = KotlinTypeChecker.DEFAULT
        if (ErrorUtils.containsErrorType(type)) return

        if (typeChecker.equalTypes(builtIns.getBooleanType(), type)) {
            addName("b", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getIntType(), type)) {
            addName("i", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getByteType(), type)) {
            addName("byte", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getLongType(), type)) {
            addName("l", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getFloatType(), type)) {
            addName("fl", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getDoubleType(), type)) {
            addName("d", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getShortType(), type)) {
            addName("sh", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getCharType(), type)) {
            addName("c", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getStringType(), type)) {
            addName("s", validator)
        }
        else if (KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) {
            val elementType = builtIns.getArrayElementType(type)
            if (typeChecker.equalTypes(builtIns.getBooleanType(), elementType)) {
                addName("booleans", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getIntType(), elementType)) {
                addName("ints", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getByteType(), elementType)) {
                addName("bytes", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getLongType(), elementType)) {
                addName("longs", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getFloatType(), elementType)) {
                addName("floats", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getDoubleType(), elementType)) {
                addName("doubles", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getShortType(), elementType)) {
                addName("shorts", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getCharType(), elementType)) {
                addName("chars", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getStringType(), elementType)) {
                addName("strings", validator)
            }
            else {
                val classDescriptor = TypeUtils.getClassDescriptor(elementType)
                if (classDescriptor != null) {
                    val className = classDescriptor.getName()
                    addName("arrayOf" + StringUtil.capitalize(className.asString()) + "s", validator)
                }
            }
        }
        else if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type)) {
            addName("function", validator)
        }
        else {
            val descriptor = type.getConstructor().getDeclarationDescriptor()
            if (descriptor != null) {
                val className = descriptor.getName()
                if (!className.isSpecial()) {
                    addCamelNames(className.asString(), validator)
                }
            }
        }
    }

    private val ACCESSOR_PREFIXES = arrayOf("get", "is", "set")

    public fun getCamelNames(name: String, validator: (String) -> Boolean, startLowerCase: Boolean): List<String> {
        val result = ArrayList<String>()
        result.addCamelNames(name, validator, startLowerCase)
        return result
    }

    private fun MutableCollection<String>.addCamelNames(name: String, validator: (String) -> Boolean, startLowerCase: Boolean = true) {
        if (name === "") return
        var s = extractIdentifiers(name)

        for (prefix in ACCESSOR_PREFIXES) {
            if (!s.startsWith(prefix)) continue

            val len = prefix.length()
            if (len < s.length() && Character.isUpperCase(s.charAt(len))) {
                s = s.substring(len)
                break
            }
        }

        var upperCaseLetterBefore = false
        for (i in 0..s.length() - 1) {
            val c = s.charAt(i)
            val upperCaseLetter = Character.isUpperCase(c)

            if (i == 0) {
                addName(if (startLowerCase) s.decapitalizeSmart() else s, validator)
            }
            else {
                if (upperCaseLetter && !upperCaseLetterBefore) {
                    val substring = s.substring(i)
                    addName(if (startLowerCase) substring.decapitalizeSmart() else substring, validator)
                }
            }

            upperCaseLetterBefore = upperCaseLetter
        }
    }

    private fun extractIdentifiers(s: String): String {
        return buildString {
            val lexer = KotlinLexer()
            lexer.start(s)
            while (lexer.tokenType != null) {
                if (lexer.tokenType == KtTokens.IDENTIFIER) {
                    append(lexer.tokenText)
                }
                lexer.advance()
            }
        }
    }

    private fun MutableCollection<String>.addNamesByExpressionPSI(expression: KtExpression?, validator: (String) -> Boolean) {
        if (expression == null) return
        val deparenthesized = KtPsiUtil.safeDeparenthesize(expression)
        when (deparenthesized) {
            is KtSimpleNameExpression -> addCamelNames(deparenthesized.getReferencedName(), validator)
            is KtQualifiedExpression -> addNamesByExpressionPSI(deparenthesized.selectorExpression, validator)
            is KtCallExpression -> addNamesByExpressionPSI(deparenthesized.calleeExpression, validator)
            is KtPostfixExpression -> addNamesByExpressionPSI(deparenthesized.baseExpression, validator)
        }
    }

    private fun MutableCollection<String>.addNamesByExpression(
            expression: KtExpression?,
            bindingContext: BindingContext?,
            validator: (String) -> Boolean
    ) {
        if (expression == null) return

        addNamesByValueArgument(expression, bindingContext, validator)
        addNamesByExpressionPSI(expression, validator)
    }

    private fun MutableCollection<String>.addNamesByValueArgument(
            expression: KtExpression,
            bindingContext: BindingContext?,
            validator: (String) -> Boolean
    ) {
        if (bindingContext == null) return
        val argumentExpression = expression.getOutermostParenthesizerOrThis()
        val valueArgument = argumentExpression.parent as? KtValueArgument ?: return
        val resolvedCall = argumentExpression.getParentResolvedCall(bindingContext) ?: return
        val argumentMatch = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return
        val parameter = argumentMatch.valueParameter
        if (parameter.containingDeclaration.hasStableParameterNames()) {
            addName(parameter.name.asString(), validator)
        }
    }

    private fun MutableCollection<String>.addName(name: String?, validator: (String) -> Boolean) {
        if (name == null) return
        val correctedName = when {
            isIdentifier(name) -> name
            name == "class" -> "clazz"
            else -> return
        }
        add(suggestNameByName(correctedName, validator))
    }

    public fun isIdentifier(name: String?): Boolean {
        if (name == null || name.isEmpty()) return false

        val lexer = KotlinLexer()
        lexer.start(name, 0, name.length())
        if (lexer.getTokenType() !== KtTokens.IDENTIFIER) return false
        lexer.advance()
        return lexer.getTokenType() == null
    }
}

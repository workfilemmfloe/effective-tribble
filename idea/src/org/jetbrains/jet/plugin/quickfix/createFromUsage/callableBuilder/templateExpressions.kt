package org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder

import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Result
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInsight.lookup.LookupElement
import java.util.LinkedHashSet
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.psi.JetParameter
import java.util.HashSet
import org.jetbrains.jet.plugin.refactoring.CollectingValidator
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.psi.JetCallableDeclaration
import java.util.Collections
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetParameterList
import org.jetbrains.jet.lang.psi.JetNamedDeclaration

/**
 * Special <code>Expression</code> for parameter names based on its type.
 */
private class ParameterNameExpression(
        private val names: Array<String>,
        private val parameterTypeToNamesMap: Map<String, Array<String>>) : Expression() {
    {
        assert(names all { it.isNotEmpty() })
    }

    override fun calculateResult(context: ExpressionContext?): Result? {
        val lookupItems = calculateLookupItems(context)!!
        return TextResult(if (lookupItems.isEmpty()) "" else lookupItems.first().getLookupString())
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?): Array<LookupElement>? {
        context!!
        val names = LinkedHashSet<String>(this.names.toList())

        // find the parameter list
        val project = context.getProject()!!
        val offset = context.getStartOffset()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.getEditor()!!
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as JetFile
        val elementAt = file.findElementAt(offset)
        val declaration = PsiTreeUtil.getParentOfType(elementAt, javaClass<JetFunction>(), javaClass<JetClass>()) ?: return array<LookupElement>()
        val parameterList = when (declaration) {
            is JetFunction -> declaration.getValueParameterList()
            is JetClass -> declaration.getPrimaryConstructorParameterList()
            else -> throw AssertionError("Unexpected declaration: ${declaration.getText()}")
        }

        // add names based on selected type
        val parameter = PsiTreeUtil.getParentOfType(elementAt, javaClass<JetParameter>())
        if (parameter != null) {
            val parameterTypeRef = parameter.getTypeReference()
            if (parameterTypeRef != null) {
                val suggestedNamesBasedOnType = parameterTypeToNamesMap[parameterTypeRef.getText()]
                if (suggestedNamesBasedOnType != null) {
                    names.addAll(suggestedNamesBasedOnType)
                }
            }
        }

        // remember other parameter names for later use
        val parameterNames = parameterList.getParameters().stream().map { jetParameter ->
            if (jetParameter == parameter) null else jetParameter.getName()
        }.filterNotNullTo(HashSet<String>())

        // add fallback parameter name
        if (names.isEmpty()) {
            names.add("arg")
        }

        // ensure there are no conflicts
        val validator = CollectingValidator(parameterNames)
        return names.map { LookupElementBuilder.create(validator.validateName(it)) }.copyToArray()
    }
}

/**
 * An <code>Expression</code> for type references and delegation specifiers.
 */
private abstract class TypeExpression(public val typeCandidates: List<TypeCandidate>) : Expression() {
    class ForTypeReference(typeCandidates: List<TypeCandidate>) : TypeExpression(typeCandidates) {
        override val cachedLookupElements: Array<LookupElement> =
                typeCandidates.map { LookupElementBuilder.create(it, it.renderedType!!) }.copyToArray()
    }

    class ForDelegationSpecifier(typeCandidates: List<TypeCandidate>) : TypeExpression(typeCandidates) {
        override val cachedLookupElements: Array<LookupElement> =
                typeCandidates.map {
                    val descriptor = it.theType.getConstructor().getDeclarationDescriptor() as ClassDescriptor
                    val text = it.renderedType!! + if (descriptor.getKind() == ClassKind.TRAIT) "" else "()"
                    LookupElementBuilder.create(it, text)
                }.copyToArray()
    }

    protected abstract val cachedLookupElements: Array<LookupElement>

    override fun calculateResult(context: ExpressionContext?): Result {
        val lookupItems = calculateLookupItems(context)
        return TextResult(if (lookupItems.size == 0) "" else lookupItems[0].getLookupString())
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?) = cachedLookupElements
}

/**
 * A sort-of dummy <code>Expression</code> for parameter lists, to allow us to update the parameter list as the user makes selections.
 */
private class TypeParameterListExpression(private val mandatoryTypeParameters: List<RenderedTypeParameter>,
                                          private val parameterTypeToTypeParameterNamesMap: Map<String, List<RenderedTypeParameter>>,
                                          insertLeadingSpace: Boolean) : Expression() {
    private val prefix = if (insertLeadingSpace) " <" else "<"

    public var currentTypeParameters: List<TypeParameterDescriptor> = Collections.emptyList()
        private set

    override fun calculateResult(context: ExpressionContext?): Result {
        context!!
        val project = context.getProject()!!
        val offset = context.getStartOffset()

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.getEditor()!!
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as JetFile
        val elementAt = file.findElementAt(offset)
        val declaration = PsiTreeUtil.getParentOfType(elementAt, javaClass<JetNamedDeclaration>()) ?: return TextResult("")

        val renderedTypeParameters = LinkedHashSet<RenderedTypeParameter>()
        renderedTypeParameters.addAll(mandatoryTypeParameters)
        for (parameter in declaration.getValueParameters()) {
            val parameterTypeRef = parameter.getTypeReference()
            if (parameterTypeRef != null) {
                val typeParameterNamesFromParameter = parameterTypeToTypeParameterNamesMap[parameterTypeRef.getText()]
                if (typeParameterNamesFromParameter != null) {
                    renderedTypeParameters.addAll(typeParameterNamesFromParameter)
                }
            }
        }
        val returnTypeRef = declaration.getReturnTypeReference()
        if (returnTypeRef != null) {
            val typeParameterNamesFromReturnType = parameterTypeToTypeParameterNamesMap[returnTypeRef.getText()]
            if (typeParameterNamesFromReturnType != null) {
                renderedTypeParameters.addAll(typeParameterNamesFromReturnType)
            }
        }


        val sortedRenderedTypeParameters = renderedTypeParameters.sortBy { if (it.fake) it.typeParameter.getIndex() else -1}
        currentTypeParameters = sortedRenderedTypeParameters.map { it.typeParameter }

        return TextResult(
                if (sortedRenderedTypeParameters.empty) "" else sortedRenderedTypeParameters.map { it.text }.joinToString(", ", prefix, ">")
        )
    }

    override fun calculateQuickResult(context: ExpressionContext?): Result = calculateResult(context)

    // do not offer the user any choices
    override fun calculateLookupItems(context: ExpressionContext?) = array<LookupElement>()
}

private object ValVarExpression: Expression() {
    private val cachedLookupElements = listOf("val", "var").map { LookupElementBuilder.create(it) }.copyToArray<LookupElement>()

    override fun calculateResult(context: ExpressionContext?): Result? = TextResult("val")

    override fun calculateQuickResult(context: ExpressionContext?): Result? = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?): Array<LookupElement>? = cachedLookupElements
}
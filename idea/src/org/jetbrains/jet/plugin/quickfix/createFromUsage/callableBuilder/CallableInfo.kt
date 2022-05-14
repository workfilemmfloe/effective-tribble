package org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder

import java.util.Collections
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.JetType
import kotlin.properties.Delegates
import com.intellij.util.ArrayUtil
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import org.jetbrains.jet.plugin.refactoring.EmptyValidator
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.util.supertypes
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass.ClassKind
import org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass.ClassInfo
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor

/**
 * Represents a concrete type or a set of types yet to be inferred from an expression.
 */
abstract class TypeInfo(val variance: Variance) {
    object Empty: TypeInfo(Variance.INVARIANT) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> = Collections.emptyList()
    }

    class ByExpression(val expression: JetExpression, variance: Variance): TypeInfo(variance) {
        override val possibleNamesFromExpression: Array<String> by Delegates.lazy {
            JetNameSuggester.suggestNamesForExpression(expression, EmptyValidator)
        }

        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                expression.guessTypes(builder.currentFileContext, builder.currentFileModule).flatMap { it.getPossibleSupertypes(variance) }
    }

    class ByTypeReference(val typeReference: JetTypeReference, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                builder.currentFileContext[BindingContext.TYPE, typeReference].getPossibleSupertypes(variance)
    }

    class ByType(val theType: JetType, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                theType.getPossibleSupertypes(variance)
    }

    class ByReceiverType(variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                (builder.placement as CallablePlacement.WithReceiver).receiverTypeCandidate.theType.getPossibleSupertypes(variance)
    }

    abstract class DelegatingTypeInfo(val delegate: TypeInfo): TypeInfo(delegate.variance) {
        override val substitutionsAllowed: Boolean = delegate.substitutionsAllowed
        override val possibleNamesFromExpression: Array<String> get() = delegate.possibleNamesFromExpression
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> = delegate.getPossibleTypes(builder)
    }

    class NoSubstitutions(delegate: TypeInfo): DelegatingTypeInfo(delegate) {
        override val substitutionsAllowed: Boolean = false
    }

    open val substitutionsAllowed: Boolean = true
    open val possibleNamesFromExpression: Array<String> get() = ArrayUtil.EMPTY_STRING_ARRAY
    abstract fun getPossibleTypes(builder: CallableBuilder): List<JetType>

    protected fun JetType?.getPossibleSupertypes(variance: Variance): List<JetType> {
        if (this == null || ErrorUtils.containsErrorType(this)) return Collections.singletonList(KotlinBuiltIns.getInstance().getAnyType())
        val single = Collections.singletonList(this)
        return when (variance) {
            Variance.IN_VARIANCE -> single + supertypes()
            else -> single
        }
    }
}

fun TypeInfo(expressionOfType: JetExpression, variance: Variance): TypeInfo = TypeInfo.ByExpression(expressionOfType, variance)
fun TypeInfo(typeReference: JetTypeReference, variance: Variance): TypeInfo = TypeInfo.ByTypeReference(typeReference, variance)
fun TypeInfo(theType: JetType, variance: Variance): TypeInfo = TypeInfo.ByType(theType, variance)

fun TypeInfo.noSubstitutions(): TypeInfo = (this as? TypeInfo.NoSubstitutions) ?: TypeInfo.NoSubstitutions(this)

fun TypeInfo.forceNotNull(): TypeInfo {
    class ForcedNotNull(delegate: TypeInfo): TypeInfo.DelegatingTypeInfo(delegate) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                super.getPossibleTypes(builder).map { it.makeNotNullable() }
    }

    return (this as? ForcedNotNull) ?: ForcedNotNull(this)
}

/**
 * Encapsulates information about a function parameter that is going to be created.
 */
class ParameterInfo(
        val typeInfo: TypeInfo,
        val preferredName: String? = null
)

enum class CallableKind {
    FUNCTION
    CONSTRUCTOR
    PROPERTY
}

abstract class CallableInfo (
        val name: String,
        val receiverTypeInfo: TypeInfo,
        val returnTypeInfo: TypeInfo,
        val possibleContainers: List<JetElement>,
        val typeParameterInfos: List<TypeInfo>
) {
    abstract val kind: CallableKind
    abstract val parameterInfos: List<ParameterInfo>
}

class FunctionInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   possibleContainers: List<JetElement> = Collections.emptyList(),
                   override val parameterInfos: List<ParameterInfo> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList()
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos) {
    override val kind: CallableKind get() = CallableKind.FUNCTION
}

class ConstructorInfo(val classInfo: ClassInfo, expectedTypeInfo: TypeInfo): CallableInfo(
        classInfo.name, TypeInfo.Empty, expectedTypeInfo.forceNotNull(), Collections.emptyList(), classInfo.typeArguments
) {
    override val kind: CallableKind get() = CallableKind.CONSTRUCTOR
    override val parameterInfos: List<ParameterInfo> get() = classInfo.parameterInfos
}

class PropertyInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   val writable: Boolean,
                   possibleContainers: List<JetElement> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList()
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos) {
    override val kind: CallableKind get() = CallableKind.PROPERTY
    override val parameterInfos: List<ParameterInfo> get() = Collections.emptyList()
}
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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.idea.search.usagesSearch.*
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchFilter.*
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetNamedFunction
import java.util.Collections
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.asJava.LightClassUtil.PropertyAccessorsPsiMethods
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.JetDeclaration
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze

val isTargetUsage = (PsiReference::matchesTarget).searchFilter

fun PsiNamedElement.getAccessorNames(readable: Boolean = true, writable: Boolean = true): List<String> {
    fun PropertyAccessorsPsiMethods.toNameList(): List<String> {
        val getter = getGetter()
        val setter = getSetter()

        val result = ArrayList<String>()
        if (readable && getter != null) result.add(getter.getName())
        if (writable && setter != null) result.add(setter.getName())
        return result
    }

    if (this !is JetDeclaration || JetPsiUtil.isLocal(this)) return Collections.emptyList()

    when (this) {
        is JetProperty ->
            return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
        is JetParameter ->
            if (hasValOrVarNode()) {
                return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
            }
    }

    return Collections.emptyList()
}

public fun PsiNamedElement.getSpecialNamesToSearch(): List<String> {
    val name = getName()
    return when {
        name == null || !Name.isValidIdentifier(name) -> Collections.emptyList<String>()
        this is JetParameter -> {
            if (!hasValOrVarNode()) return Collections.emptyList<String>()

            val context = this.analyze()
            val paramDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? ValueParameterDescriptor
            context[BindingContext.DATA_CLASS_COMPONENT_FUNCTION, paramDescriptor]?.let {
                listOf(it.getName().asString(), JetTokens.LPAR.getValue())
            } ?: Collections.emptyList<String>()
        }
        else -> Name.identifier(name).getOperationSymbolsToSearch().map { (it as JetSingleValueToken).getValue() }
    }
}

public abstract class UsagesSearchHelper<T : PsiNamedElement> {
    protected open fun makeFilter(target: UsagesSearchTarget<T>): UsagesSearchFilter = isTargetUsage

    protected open fun makeWordList(target: UsagesSearchTarget<T>): List<String> {
        return with(target.element) {
            ContainerUtil.createMaybeSingletonList(getName()) + getAccessorNames() + getSpecialNamesToSearch()
        }
    }


    protected open fun makeItemList(target: UsagesSearchTarget<T>): List<UsagesSearchRequestItem> =
            Collections.singletonList(newItem(target))

    fun newItem(target: UsagesSearchTarget<T>): UsagesSearchRequestItem {
        return UsagesSearchRequestItem(target, makeWordList(target), makeFilter(target))
    }

    fun newRequest(target: UsagesSearchTarget<T>): UsagesSearchRequest {
        return UsagesSearchRequest(target.element.getProject(), makeItemList(target))
    }
}

val isNotImportUsage = !((PsiReference::isImportUsage).searchFilter)

trait ImportAwareSearchHelper {
    public val skipImports: Boolean

    protected val isFilteredImport: UsagesSearchFilter
        get() = isNotImportUsage.ifOrTrue(skipImports)
}

open class DefaultSearchHelper<T: PsiNamedElement>(
        override val skipImports: Boolean = false
): UsagesSearchHelper<T>(), ImportAwareSearchHelper {
    override fun makeFilter(target: UsagesSearchTarget<T>): UsagesSearchFilter = isTargetUsage and isFilteredImport
}

val isClassConstructorUsage = (PsiReference::isConstructorUsage).searchFilter

class ClassUsagesSearchHelper(
        public val constructorUsages: Boolean = false,
        public val nonConstructorUsages: Boolean = false,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetClassOrObject>(skipImports) {
    override fun makeFilter(target: UsagesSearchTarget<JetClassOrObject>): UsagesSearchFilter =
            super.makeFilter(target) and when {
                constructorUsages && !nonConstructorUsages -> isClassConstructorUsage
                !constructorUsages && nonConstructorUsages -> !isClassConstructorUsage
                !constructorUsages && !nonConstructorUsages -> False
                else -> True
            }
}

class ClassDeclarationsUsagesSearchHelper(
        public val functionUsages: Boolean = false,
        public val propertyUsages: Boolean = false,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetClassOrObject>(skipImports) {
    override fun makeItemList(target: UsagesSearchTarget<JetClassOrObject>): List<UsagesSearchRequestItem> {
        val items = ArrayList<UsagesSearchRequestItem>()
        val declHelper = DefaultSearchHelper<JetNamedDeclaration>(skipImports)

        for (decl in target.element.effectiveDeclarations()) {
            if ((decl is JetNamedFunction && functionUsages) || ((decl is JetProperty || decl is JetParameter) && propertyUsages)) {
                items.add(declHelper.newItem(target.retarget(decl as JetNamedDeclaration)))
            }
        }

        return items
    }
}

val isOverrideUsage = (PsiReference::isCallableOverrideUsage).searchFilter

trait OverrideSearchHelper {
    public val selfUsages: Boolean
    public val overrideUsages: Boolean

    val isTargetOrOverrideUsage: UsagesSearchFilter
        get() = isTargetUsage.ifOrFalse(selfUsages) or isOverrideUsage.ifOrFalse(overrideUsages)
}

val isOverloadUsage = (PsiReference::isUsageInContainingDeclaration).searchFilter
val isExtensionUsage = (PsiReference::isExtensionOfDeclarationClassUsage).searchFilter

class FunctionUsagesSearchHelper(
        public val overloadUsages: Boolean = false,
        public val extensionUsages: Boolean = false,
        override val selfUsages: Boolean = true,
        override val overrideUsages: Boolean = true,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetNamedFunction>(skipImports), OverrideSearchHelper {
    override fun makeFilter(target: UsagesSearchTarget<JetNamedFunction>): UsagesSearchFilter {
        return (isTargetOrOverrideUsage
        or isOverloadUsage.ifOrFalse(overloadUsages)
        or isExtensionUsage.ifOrFalse(extensionUsages)) and isFilteredImport
    }
}

val isPropertyReadOnlyUsage = (PsiReference::isPropertyReadOnlyUsage).searchFilter

// Used for JetProperty and JetParameter
class PropertyUsagesSearchHelper(
        public val readUsages: Boolean = true,
        public val writeUsages: Boolean = true,
        public override val selfUsages: Boolean = true,
        public override val overrideUsages: Boolean = true,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetNamedDeclaration>(skipImports), OverrideSearchHelper {
    override fun makeWordList(target: UsagesSearchTarget<JetNamedDeclaration>): List<String> {
        return with(target.element) {
            ContainerUtil.createMaybeSingletonList(getName()) +
                    getAccessorNames(readable = readUsages, writable = writeUsages) +
                    getSpecialNamesToSearch()
        }
    }

    override fun makeFilter(target: UsagesSearchTarget<JetNamedDeclaration>): UsagesSearchFilter {
        val readWriteUsage = when {
            readUsages && writeUsages -> True
            readUsages -> isPropertyReadOnlyUsage
            writeUsages -> !isPropertyReadOnlyUsage
            else -> False
        }
        return isTargetOrOverrideUsage and readWriteUsage and isFilteredImport
    }
}

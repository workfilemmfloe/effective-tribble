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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDirectory
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.ConflictsUtil
import org.jetbrains.kotlin.psi.psiUtil.getPackage
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.JetFileType
import com.intellij.openapi.project.Project
import java.io.File
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.*
import java.util.ArrayList
import com.intellij.openapi.application.ApplicationManager
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.ui.ConflictsDialog
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.codeInsight.unwrap.RangeSplitter
import com.intellij.codeInsight.unwrap.UnwrapHandler
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import java.util.Collections
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.components.JBList
import com.intellij.openapi.ui.popup.JBPopupAdapter
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import com.intellij.openapi.util.text.StringUtil
import javax.swing.Icon
import org.jetbrains.kotlin.idea.util.string.collapseSpaces
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.psiUtil.parents

fun <T: Any> PsiElement.getAndRemoveCopyableUserData(key: Key<T>): T? {
    val data = getCopyableUserData(key)
    putCopyableUserData(key, null)
    return data
}

fun getOrCreateKotlinFile(fileName: String, targetDir: PsiDirectory): JetFile? =
        (targetDir.findFile(fileName) ?: createKotlinFile(fileName, targetDir)) as? JetFile

fun createKotlinFile(fileName: String, targetDir: PsiDirectory): JetFile {
    val packageName = targetDir.getPackage()?.getQualifiedName()

    targetDir.checkCreateFile(fileName)
    val file = PsiFileFactory.getInstance(targetDir.getProject()).createFileFromText(
            fileName, JetFileType.INSTANCE, if (packageName != null && packageName.isNotEmpty()) "package $packageName \n\n" else ""
    )

    return targetDir.add(file) as JetFile
}

public fun File.toVirtualFile(): VirtualFile? = LocalFileSystem.getInstance().findFileByIoFile(this)

public fun File.toPsiFile(project: Project): PsiFile? {
    return toVirtualFile()?.let { vfile -> PsiManager.getInstance(project).findFile(vfile) }
}

public fun File.toPsiDirectory(project: Project): PsiDirectory? {
    return toVirtualFile()?.let { vfile -> PsiManager.getInstance(project).findDirectory(vfile) }
}

public fun PsiElement.getUsageContext(): PsiElement {
    return when (this) {
        is JetElement -> PsiTreeUtil.getParentOfType(this, javaClass<JetNamedDeclaration>(), javaClass<JetFile>())!!
        else -> ConflictsUtil.getContainer(this)
    }
}

public fun PsiElement.isInJavaSourceRoot(): Boolean =
        !JavaProjectRootsUtil.isOutsideJavaSourceRoot(getContainingFile())

public inline fun JetFile.createTempCopy(textTransform: (String) -> String): JetFile {
    val tmpFile = JetPsiFactory(this).createAnalyzableFile(getName(), textTransform(getText() ?: ""), this)
    tmpFile.setOriginalFile(this)
    tmpFile.suppressDiagnosticsInDebugMode = suppressDiagnosticsInDebugMode
    return tmpFile
}

public fun PsiElement.getAllExtractionContainers(strict: Boolean = true): List<JetElement> {
    val containers = ArrayList<JetElement>()

    var element: PsiElement? = if (strict) getParent() else this
    while (element != null) {
        when (element) {
            is JetBlockExpression, is JetClassBody, is JetFile -> containers.add(element as JetElement)
        }

        element = element!!.getParent()
    }

    return containers
}

public fun PsiElement.getExtractionContainers(strict: Boolean = true, includeAll: Boolean = false): List<JetElement> {
    fun getEnclosingDeclaration(element: PsiElement, strict: Boolean): PsiElement? {
        return element.parents(!strict)
                .filter {
                    (it is JetDeclarationWithBody && it !is JetFunctionLiteral)
                    || it is JetClassInitializer
                    || it is JetClassBody
                    || it is JetFile
                }
                .firstOrNull()
    }

    if (includeAll) return getAllExtractionContainers(strict)

    val enclosingDeclaration = getEnclosingDeclaration(this, strict)?.let {
        if (it is JetDeclarationWithBody || it is JetClassInitializer) getEnclosingDeclaration(it, true) else it
    }

    return when (enclosingDeclaration) {
        is JetFile -> Collections.singletonList(enclosingDeclaration)
        is JetClassBody -> getAllExtractionContainers(strict).filterIsInstance<JetClassBody>()
        else -> {
            val targetContainer = when (enclosingDeclaration) {
                is JetDeclarationWithBody -> enclosingDeclaration.getBodyExpression()
                is JetClassInitializer -> enclosingDeclaration.getBody()
                else -> null
            }
            if (targetContainer is JetBlockExpression) Collections.singletonList(targetContainer) else Collections.emptyList()
        }
    }
}

public fun Project.checkConflictsInteractively(conflicts: MultiMap<PsiElement, String>, onAccept: () -> Unit) {
    if (!conflicts.isEmpty()) {
        if (ApplicationManager.getApplication()!!.isUnitTestMode()) throw ConflictsInTestsException(conflicts.values())

        val dialog = ConflictsDialog(this, conflicts, onAccept)
        dialog.show()
        if (!dialog.isOK()) return
    }

    onAccept()
}

public fun <T, E: PsiElement> getPsiElementPopup(
        editor: Editor,
        elements: List<T>,
        renderer: PsiElementListCellRenderer<E>,
        title: String?,
        highlightSelection: Boolean,
        toPsi: (T) -> E,
        processor: (T) -> Boolean): JBPopup {
    val highlighter = if (highlightSelection) SelectionAwareScopeHighlighter(editor) else null

    val list = JBList(elements.map(toPsi))
    list.setCellRenderer(renderer)
    list.addListSelectionListener { e ->
        highlighter?.dropHighlight()
        val index = list.getSelectedIndex()
        if (index >= 0) {
            highlighter?.highlight(list.getModel()!!.getElementAt(index) as PsiElement)
        }
    }

    return with(PopupChooserBuilder(list)) {
        title?.let { setTitle(it) }
        renderer.installSpeedSearch(this, true)
        setItemChoosenCallback {
            val index = list.getSelectedIndex()
            if (index >= 0) {
                processor(elements[index])
            }
        }
        addListener(object: JBPopupAdapter() {
            override fun onClosed(event: LightweightWindowEvent?) {
                highlighter?.dropHighlight();
            }
        })

        createPopup()
    }
}

public class SelectionAwareScopeHighlighter(val editor: Editor) {
    private val highlighters = ArrayList<RangeHighlighter>()

    private fun addHighlighter(r: TextRange, attr: TextAttributes) {
        highlighters.add(
                editor.getMarkupModel().addRangeHighlighter(
                        r.getStartOffset(),
                        r.getEndOffset(),
                        UnwrapHandler.HIGHLIGHTER_LEVEL,
                        attr,
                        HighlighterTargetArea.EXACT_RANGE
                )
        )
    }

    public fun highlight(wholeAffected: PsiElement) {
        dropHighlight()

        val attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)!!
        val selectedRange = with(editor.getSelectionModel()) { TextRange(getSelectionStart(), getSelectionEnd()) }
        for (r in RangeSplitter.split(wholeAffected.getTextRange()!!, Collections.singletonList(selectedRange))) {
            addHighlighter(r, attributes)
        }
    }

    public fun dropHighlight() {
        highlighters.forEach { it.dispose() }
        highlighters.clear()
    }
}

fun PsiElement.getLineCount(): Int {
    val doc = getContainingFile()?.let { file -> PsiDocumentManager.getInstance(getProject()).getDocument(file) }
    if (doc != null) {
        val spaceRange = getTextRange() ?: TextRange.EMPTY_RANGE

        val startLine = doc.getLineNumber(spaceRange.getStartOffset())
        val endLine = doc.getLineNumber(spaceRange.getEndOffset())

        return endLine - startLine
    }

    return (getText() ?: "").count { it == '\n' } + 1
}

fun PsiElement.isMultiLine(): Boolean = getLineCount() > 1

public fun JetElement.getContextForContainingDeclarationBody(): BindingContext? {
    val enclosingDeclaration = getStrictParentOfType<JetDeclaration>()
    val bodyElement = when (enclosingDeclaration) {
        is JetDeclarationWithBody -> enclosingDeclaration.getBodyExpression()
        is JetWithExpressionInitializer -> enclosingDeclaration.getInitializer()
        is JetMultiDeclaration -> enclosingDeclaration.getInitializer()
        is JetParameter -> enclosingDeclaration.getDefaultValue()
        is JetClassInitializer -> enclosingDeclaration.getBody()
        is JetClass -> {
            val delegationSpecifierList = enclosingDeclaration.getDelegationSpecifierList()
            if (delegationSpecifierList.isAncestor(this)) this else null
        }
        else -> null
    }
    return bodyElement?.let { it.analyze() }
}

public fun chooseContainerElement<T>(
        containers: List<T>,
        editor: Editor,
        title: String,
        highlightSelection: Boolean,
        toPsi: (T) -> JetElement,
        onSelect: (T) -> Unit) {
    return getPsiElementPopup(
            editor,
            containers,
            object : PsiElementListCellRenderer<JetElement>() {
                private fun JetElement.renderName(): String {
                    if (this is JetPropertyAccessor) {
                        return (getParent() as JetProperty).renderName() + if (isGetter()) ".get" else ".set"
                    }
                    if (this is JetObjectDeclaration && this.isClassObject()) {
                        return "Class object of ${getStrictParentOfType<JetClassOrObject>()?.renderName() ?: "<anonymous>"}"
                    }
                    return (this as? PsiNamedElement)?.getName() ?: "<anonymous>"
                }

                private fun JetElement.renderDeclaration(): String? {
                    val name = renderName()
                    val descriptor = this.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
                    val params = (descriptor as? FunctionDescriptor)?.let { descriptor ->
                        descriptor.getValueParameters()
                                .map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it.getType()) }
                                .joinToString(", ", "(", ")")
                    } ?: ""
                    return "$name$params"
                }

                private fun JetElement.renderText(): String {
                    return StringUtil.shortenTextWithEllipsis(getText()!!.collapseSpaces(), 53, 0)
                }

                private fun JetElement.getRepresentativeElement(): JetElement {
                    return when (this) {
                        is JetBlockExpression -> (getParent() as? JetDeclarationWithBody) ?: this
                        is JetClassBody -> getParent() as JetClassOrObject
                        else -> this
                    }
                }

                override fun getElementText(element: JetElement): String? {
                    val representativeElement = element.getRepresentativeElement()
                    return when (representativeElement) {
                        is JetFile, is JetDeclarationWithBody, is JetClassOrObject -> representativeElement.renderDeclaration()
                        else -> representativeElement.renderText()
                    }
                }

                override fun getContainerText(element: JetElement?, name: String?): String? = null

                override fun getIconFlags(): Int = 0

                override fun getIcon(element: PsiElement?): Icon? =
                        super.getIcon((element as? JetElement)?.getRepresentativeElement())
            },
            title,
            highlightSelection,
            toPsi,
            {
                onSelect(it)
                true
            }
    ).showInBestPositionFor(editor)
}

public fun chooseContainerElementIfNecessary<T>(
        containers: List<T>,
        editor: Editor,
        title: String,
        highlightSelection: Boolean,
        toPsi: (T) -> JetElement,
        onSelect: (T) -> Unit
) {
    when {
        containers.isEmpty() -> return
        containers.size() == 1 || ApplicationManager.getApplication()!!.isUnitTestMode() -> onSelect(containers.first())
        else -> chooseContainerElement(containers, editor, title, highlightSelection, toPsi, onSelect)
    }
}

public fun PsiElement.isTrueJavaMethod(): Boolean = this is PsiMethod && this !is KotlinLightMethod

fun compareDescriptors(d1: DeclarationDescriptor?, d2: DeclarationDescriptor?): Boolean {
    return d1 == d2 ||
           (d1 != null && d2 != null &&
            DescriptorRenderer.FQ_NAMES_IN_TYPES.render(d1) == DescriptorRenderer.FQ_NAMES_IN_TYPES.render(d2))
}

public fun comparePossiblyOverridingDescriptors(currentDescriptor: DeclarationDescriptor?, originalDescriptor: DeclarationDescriptor?): Boolean {
    if (compareDescriptors(currentDescriptor, originalDescriptor)) return true
    if (originalDescriptor is CallableDescriptor) {
        if (!OverridingUtil.traverseOverridenDescriptors(originalDescriptor) { !compareDescriptors(currentDescriptor, it) }) return true
    }

    return false
}

public fun PsiElement.canRefactor(): Boolean {
    return when (this) {
        is PsiPackage ->
            getDirectories().any { it.canRefactor() }
        is JetElement, is PsiDirectory ->
            isWritable() && ProjectRootsUtil.isInProjectSource(this)
        else ->
            false
    }
}

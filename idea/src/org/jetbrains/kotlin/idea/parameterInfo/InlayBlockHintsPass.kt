/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeHighlighting.EditorBoundHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager
import com.intellij.codeInsight.hints.*
import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.ex.util.CaretVisualPositionKeeper
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.xdebugger.ui.DebuggerColors
import gnu.trove.TIntObjectHashMap
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle
import java.util.*

@Suppress("UnstableApiUsage")
class InlayBlockHintsPass(private val myRootElement: PsiElement, editor: Editor) :
    EditorBoundHighlightingPass(editor, myRootElement.containingFile, true) {

    private val myHints = TIntObjectHashMap<List<ParameterHintsPass.HintData>>()
    private val myInlays = HashSet<InlayInfo>()
    private val myShowOnlyIfExistedBeforeHints = TIntObjectHashMap<String>()
    private val myTraverser: SyntaxTraverser<PsiElement>

    init {
        myTraverser = SyntaxTraverser.psiTraverser(myRootElement)
    }

    override fun doCollectInformation(progress: ProgressIndicator) {
        assert(myDocument != null)
        return

        if (myFile.language != KotlinLanguage.INSTANCE) return

        myHints.clear()
        myInlays.clear()

        val provider = InlayParameterHintsExtension.forLanguage(KotlinLanguage.INSTANCE)
        if (provider == null || !provider.canShowHintsWhenDisabled() && !isEnabled || DiffUtil.isDiffEditor(myEditor)) return

        myTraverser.forEach { element -> process(element) }
    }

    private fun process(element: PsiElement) {
        val hints = LAMBDA_RETURN_EXPRESSION_HINT.provideHints(element)
        if (hints.isEmpty()) return

//        val info = provider.getHintInfo(element)
//        val showHints = info == null || info is HintInfo.OptionInfo

        var inlays = hints.stream()
//        if (!showHints) {
//            inlays = inlays.filter { inlayInfo -> !inlayInfo.isFilterByBlacklist }
//        }

        inlays.forEach { hint ->
            val offset = hint.offset

            if (!canShowHintsAtOffset(offset)) return@forEach

            if (hint is InlayInfo) {
                myInlays.add(hint)
            }

//
//            val presentation = provider.getInlayPresentation(hint.text)
//            if (hint.isShowOnlyIfExistedBefore) {
//                myShowOnlyIfExistedBeforeHints.put(offset, presentation)
//            } else {
//                val hintList: MutableList<ParameterHintsPass.HintData> = (myHints.get(offset) as? MutableList<ParameterHintsPass.HintData>) ?: let {
//                    val newList = ArrayList<ParameterHintsPass.HintData>()
//                    myHints.put(offset, newList)
//                    newList
//                }
//
//
//
//                val widthAdjustment = convertHintPresentation(hint.widthAdjustment, provider)
//                hintList.add(ParameterHintsPass.HintData(presentation, hint.relatesToPrecedingText, widthAdjustment))
//            }
        }
    }

    override fun doApplyInformationToEditor() {
        return

        val keeper = CaretVisualPositionKeeper(myEditor)
        val manager = ParameterHintsPresentationManager.getInstance()
//        val hints: List<Inlay<*>> = hintsInRootElementArea(manager)

        for (inlay in myEditor.inlayModel.getBlockElementsInRange(0, myEditor.document.textLength, MyBlockRendererPass::class.java)) {
            Disposer.dispose(inlay)
        }

        for (hint in myInlays) {
            val blockElementsInRange = myEditor.inlayModel.getBlockElementsInRange(hint.offset, hint.offset)
            if (blockElementsInRange.isEmpty()) {
//                val lineStartOffset = DocumentUtil.getLineStartOffset(hint.offset, myEditor.getDocument())

                val inlayText = if (hint.text.startsWith(TYPE_INFO_PREFIX)) {
                    hint.text.substring(TYPE_INFO_PREFIX.length)
                } else {
                    hint.text
                }

                myEditor.inlayModel.addBlockElement(
                    hint.offset,
                    hint.relatesToPrecedingText,
                    false,
                    0,
                    MyBlockRendererPass(hint.offset, inlayText)
                )
            }
        }

//        val updater = ParameterHintsUpdater(myEditor, hints, myHints, myShowOnlyIfExistedBeforeHints, true)
//        updater.update()

        keeper.restoreOriginalLocation(false)
    }

    private fun hintsInRootElementArea(manager: ParameterHintsPresentationManager): List<Inlay<*>> {
        assert(myDocument != null)

        val range = myRootElement.textRange
        val elementStart = range.startOffset
        val elementEnd = range.endOffset

        return manager.getParameterHintsInRange(myEditor, elementStart + 1, elementEnd - 1)
    }

    /**
     * Adding hints on the borders of root element (at startOffset or endOffset)
     * is allowed only in the case when root element is a document
     *
     * @return true iff a given offset can be used for hint rendering
     */
    private fun canShowHintsAtOffset(offset: Int): Boolean {
        val rootRange = myRootElement.textRange

        if (!rootRange.containsOffset(offset)) return false
        return if (offset > rootRange.startOffset && offset < rootRange.endOffset) true else myDocument != null && myDocument.textLength == rootRange.length

    }

    companion object {
        class Factory(registrar: TextEditorHighlightingPassRegistrar) : ProjectComponent, TextEditorHighlightingPassFactory {
            init {
                registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
            }

            override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
                if (file !is KtFile) return null
                return InlayBlockHintsPass(file, editor)
            }
        }

        fun syncUpdate(element: PsiElement, editor: Editor) {
            val filter = MethodInfoBlacklistFilter.forLanguage(element.language)
            val pass = ParameterHintsPass(element, editor, filter, true)
            pass.doCollectInformation(ProgressIndicatorBase())
            pass.applyInformationToEditor()
        }

        private val isEnabled: Boolean
            get() = EditorSettingsExternalizable.getInstance().isShowParameterNameHints

        private fun convertHintPresentation(widthAdjustment: HintWidthAdjustment?, provider: InlayParameterHintsProvider): HintWidthAdjustment? {
            var widthAdjustment = widthAdjustment
            if (widthAdjustment != null) {
                val hintText = widthAdjustment.hintTextToMatch
                if (hintText != null) {
                    val adjusterHintPresentation = provider.getInlayPresentation(hintText)
                    if (hintText != adjusterHintPresentation) {
                        widthAdjustment = HintWidthAdjustment(
                            widthAdjustment.editorTextToMatch,
                            adjusterHintPresentation,
                            widthAdjustment.adjustmentPosition
                        )
                    }
                }
            }

            return widthAdjustment
        }

        private class MyBlockRendererPass(val offset: Int, val value: String) : EditorCustomElementRenderer {
            override fun calcHeightInPixels(inlay: Inlay<*>): Int {
                return super.calcHeightInPixels(inlay) * 2 / 3
            }

            override fun calcWidthInPixels(inlay: Inlay<*>): Int {
                return 0
            }

            override fun paint(
                inlay: Inlay<*>,
                g: Graphics,
                targetRegion: Rectangle,
                textAttributes: TextAttributes
            ) {
                val editor = inlay.editor

                val xStart = editor.offsetToXY(offset, true, false).x

                val colorsScheme = editor.colorsScheme
                val attributes = colorsScheme.getAttributes(DebuggerColors.INLINED_VALUES_EXECUTION_LINE) ?: return
                val fgColor = attributes.foregroundColor ?: return

                g.color = fgColor
                g.font = Font(colorsScheme.editorFontName, attributes.fontType, colorsScheme.editorFontSize * 2 / 3)

                g.drawString(value, xStart, targetRegion.y + (editor as EditorImpl).ascent * 2 / 3)
            }
        }

        private object LAMBDA_RETURN_EXPRESSION_HINT {
            fun isApplicable(elem: PsiElement) =
                elem is KtExpression && elem !is KtFunctionLiteral && !elem.isNameReferenceInCall()

            fun provideHints(elem: PsiElement): List<InlayInfo> {
                if (elem is KtExpression) {
                    return provideLambdaReturnValueHints(elem)
                }

                return emptyList()
            }
        }
    }
}

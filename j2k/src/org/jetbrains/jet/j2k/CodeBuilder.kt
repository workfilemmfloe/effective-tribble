/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import com.intellij.psi.*
import java.util.HashSet
import org.jetbrains.jet.lang.psi.psiUtil.isAncestor
import java.util.ArrayList
import org.jetbrains.jet.j2k.ast.Element
import org.jetbrains.jet.j2k.ast.Modifiers
import kotlin.platform.platformName

fun<T> CodeBuilder.append(generators: Collection<() -> T>, separator: String, prefix: String = "", suffix: String = ""): CodeBuilder {
    if (generators.isNotEmpty()) {
        append(prefix)
        var first = true
        for (generator in generators) {
            if (!first) {
                append(separator)
            }
            generator()
            first = false
        }
        append(suffix)
    }
    return this
}

platformName("appendElements")
fun CodeBuilder.append(elements: Collection<Element>, separator: String, prefix: String = "", suffix: String = ""): CodeBuilder {
    return append(elements.filter { !it.isEmpty }.map { { append(it) } }, separator, prefix, suffix)
}

class CodeBuilder(private val topElement: PsiElement?) {
    private val builder = StringBuilder()
    private var endOfLineCommentAtEnd = false

    private val commentsAndSpacesUsed = HashSet<PsiElement>()

    public fun append(text: String, endOfLineComment: Boolean = false): CodeBuilder {
        if (text.isEmpty()) {
            assert(!endOfLineComment)
            return this
        }

        if (endOfLineCommentAtEnd) {
            if (text[0] != '\n' && text[0] != '\r') builder.append('\n')
            endOfLineCommentAtEnd = false
        }

        builder.append(text)
        endOfLineCommentAtEnd = endOfLineComment
        return this
    }

    public val result: String
        get() = builder.toString()

    public fun append(element: Element): CodeBuilder {
        if (element.isEmpty) return this // do not insert comment and spaces for empty elements to avoid multiple blank lines

        if (element.prototypes.isEmpty() || topElement == null) {
            element.generateCode(this)
            return this
        }

        val prefixElements = ArrayList<PsiElement>(2)
        val postfixElements = ArrayList<PsiElement>(2)
        for ((prototype, inheritBlankLinesBefore) in element.prototypes) {
            assert(prototype !is PsiComment)
            assert(prototype !is PsiWhiteSpace)
            assert(topElement.isAncestor(prototype))
            prefixElements.collectPrefixElements(prototype, inheritBlankLinesBefore)
            postfixElements.collectPostfixElements(prototype)
        }

        commentsAndSpacesUsed.addAll(prefixElements)
        commentsAndSpacesUsed.addAll(postfixElements)

        for (i in prefixElements.indices) {
            val e = prefixElements[i]
            if (i == 0 && e is PsiWhiteSpace) {
                if (e.newLinesCount() > 1) {  // insert at maximum one blank line
                    append("\n")
                }
            }
            else {
                append(e.getText()!!, e.isEndOfLineComment())
            }
        }

        element.generateCode(this)

        // scan for all comments inside which are not yet used in the text and put them here to not loose any comment from code
        for ((prototype, _) in element.prototypes) {
            prototype.accept(object : JavaRecursiveElementVisitor(){
                override fun visitComment(comment: PsiComment) {
                    if (commentsAndSpacesUsed.add(comment)) {
                        append(comment.getText()!!, comment.isEndOfLineComment())
                    }
                }
            })
        }

        postfixElements.forEach { append(it.getText()!!, it.isEndOfLineComment()) }

        return this
    }

    private fun MutableList<PsiElement>.collectPrefixElements(element: PsiElement, allowBlankLinesBefore: Boolean) {
        val atStart = ArrayList<PsiElement>(2).collectCommentsAndSpacesAtStart(element)

        val before = ArrayList<PsiElement>(2).collectCommentsAndSpacesBefore(element)
        if (!allowBlankLinesBefore && before.lastOrNull() is PsiWhiteSpace) {
            before.remove(before.size - 1)
        }

        addAll(before.reverse())
        addAll(atStart)
    }

    private fun MutableList<PsiElement>.collectPostfixElements(element: PsiElement) {
        val atEnd = ArrayList<PsiElement>(2).collectCommentsAndSpacesAtEnd(element)

        val after = ArrayList<PsiElement>(2).collectCommentsAndSpacesAfter(element)
        if (after.isNotEmpty()) {
            val last = after.last()
            if (last is PsiWhiteSpace) {
                after.remove(after.size - 1)
            }
        }

        addAll(atEnd.reverse())
        addAll(after)
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesBefore(element: PsiElement): MutableList<PsiElement> {
        if (element == topElement) return this

        val prev = element.getPrevSibling()
        if (prev != null) {
            if (prev.isCommentOrSpace()) {
                if (prev !in commentsAndSpacesUsed) {
                    add(prev)
                    collectCommentsAndSpacesBefore(prev)
                }
            }
            else if (prev.isEmptyElement()){
                collectCommentsAndSpacesBefore(prev)
            }
        }
        else {
            collectCommentsAndSpacesBefore(element.getParent()!!)
        }
        return this
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesAfter(element: PsiElement): MutableList<PsiElement> {
        if (element == topElement) return this

        val next = element.getNextSibling()
        if (next != null) {
            if (next.isCommentOrSpace()) {
                if (next is PsiWhiteSpace && next.hasNewLines()) return this // do not attach anything on next line after element
                if (next !in commentsAndSpacesUsed) {
                    add(next)
                    collectCommentsAndSpacesAfter(next)
                }
            }
            else if (next.isEmptyElement()){
                collectCommentsAndSpacesAfter(next)
            }
        }
        else {
            collectCommentsAndSpacesAfter(element.getParent()!!)
        }
        return this
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesAtStart(element: PsiElement): MutableList<PsiElement> {
        var child = element.getFirstChild()
        while(child != null) {
            if (child!!.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child!!) else break
            }
            else if (!child!!.isEmptyElement()) {
                collectCommentsAndSpacesAtStart(child!!)
                break
            }
            child = child!!.getNextSibling()
        }
        return this
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesAtEnd(element: PsiElement): MutableList<PsiElement> {
        var child = element.getLastChild()
        while(child != null) {
            if (child!!.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child!!) else break
            }
            else if (!child!!.isEmptyElement()) {
                collectCommentsAndSpacesAtEnd(child!!)
                break
            }
            child = child!!.getPrevSibling()
        }
        return this
    }

    private fun PsiElement.isCommentOrSpace() = this is PsiComment || this is PsiWhiteSpace

    private fun PsiElement.isEndOfLineComment() = this is PsiComment && getTokenType() == JavaTokenType.END_OF_LINE_COMMENT

    private fun PsiElement.isEmptyElement() = getFirstChild() == null && getTextLength() == 0

    private fun PsiWhiteSpace.newLinesCount() = getText()!!.count { it == '\n' } //TODO: this is not correct!!

    private fun PsiWhiteSpace.hasNewLines() = getText()!!.any { it == '\n' || it == '\r' }
}


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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.JetKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

public class JetAnnotationUseSiteTarget : JetElementImplStub<KotlinAnnotationUseSiteTargetStub> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinAnnotationUseSiteTargetStub) : super(stub, JetStubElementTypes.ANNOTATION_TARGET)

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D) = visitor.visitAnnotationUseSiteTarget(this, data)

    public fun getAnnotationUseSiteTarget(): AnnotationUseSiteTarget {
        val targetString = stub?.getUseSiteTarget()
        if (targetString != null) {
            try {
                return AnnotationUseSiteTarget.valueOf(targetString)
            } catch (e: IllegalArgumentException) {
                // Ok, resolve via node tree
            }
        }

        val node = getFirstChild().getNode()
        return when (node.getElementType()) {
            JetTokens.FIELD_KEYWORD -> AnnotationUseSiteTarget.FIELD
            JetTokens.FILE_KEYWORD -> AnnotationUseSiteTarget.FILE
            JetTokens.PROPERTY_KEYWORD -> AnnotationUseSiteTarget.PROPERTY
            JetTokens.GET_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_GETTER
            JetTokens.SET_KEYWORD -> AnnotationUseSiteTarget.PROPERTY_SETTER
            JetTokens.RECEIVER_KEYWORD -> AnnotationUseSiteTarget.RECEIVER
            JetTokens.PARAM_KEYWORD -> AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
            JetTokens.SPARAM_KEYWORD -> AnnotationUseSiteTarget.SETTER_PARAMETER
            else -> throw IllegalStateException("Unknown annotation target " + node.getText())
        }
    }

}
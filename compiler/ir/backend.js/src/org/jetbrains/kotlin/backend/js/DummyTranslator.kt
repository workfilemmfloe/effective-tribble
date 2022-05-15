/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.js

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.backend.js.translate.context.StaticContext
import org.jetbrains.kotlin.backend.js.translate.context.TranslationContext
import org.jetbrains.kotlin.backend.js.translate.declaration.FileDeclarationVisitor
import org.jetbrains.kotlin.js.facade.exceptions.TranslationRuntimeException
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace

class DummyTranslator(
    private val bindingTrace: BindingTrace,
    private val module: ModuleDescriptor,
    private val config: JsConfig,
    private val sourceFilePathResolver: SourceFilePathResolver
) {

    fun translate(files: Collection<KtFile>): Collection<JsProgramFragment> {

        val fragments = mutableListOf<JsProgramFragment>()

        for (file in files) {
            val staticContext = StaticContext(bindingTrace, config, module, sourceFilePathResolver)
            val context = TranslationContext.rootContext(staticContext)
            translateFile(context, file)
            fragments += staticContext.fragment
        }

        return fragments
    }


    private fun translateFile(context: TranslationContext, file: KtFile) {
        val fileVisitor = FileDeclarationVisitor(context)

        try {
            for (declaration in file.declarations) {
                val descriptor = BindingUtils.getDescriptorForElement(context.bindingContext(), declaration)
                if (!AnnotationsUtils.isPredefinedObject(descriptor)) {
                    declaration.accept(fileVisitor, context)
                }
            }
        } catch (e: TranslationRuntimeException) {
            throw e
        } catch (e: RuntimeException) {
            throw TranslationRuntimeException(file, e)
        } catch (e: AssertionError) {
            throw TranslationRuntimeException(file, e)
        }
    }
}
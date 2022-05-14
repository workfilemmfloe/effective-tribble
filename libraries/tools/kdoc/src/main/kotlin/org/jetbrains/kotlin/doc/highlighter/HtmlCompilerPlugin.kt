package org.jetbrains.kotlin.doc.highlighter

import org.jetbrains.jet.cli.common.CompilerPlugin
import org.jetbrains.jet.cli.common.CompilerPluginContext

/**
*/
class HtmlCompilerPlugin : CompilerPlugin {
   public override fun processFiles(context: CompilerPluginContext) {
      val files = context.getFiles()
      for (file in files) {
          file.accept(HtmlKotlinVisitor())
      }
    }
}

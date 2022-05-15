/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.konan.analyser.index

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.konan.KotlinWorkaroundUtil.createDecompiledFileViewProvider
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.common.createIncompatibleAbiVersionDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ClassDeserializer
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.IOException

//todo: Fix in Kotlin plugin
abstract class KonanMetadataDecompilerBase<out V : BinaryVersion>(
  private val fileType: FileType,
  private val targetPlatform: TargetPlatform,
  private val serializerProtocol: SerializerExtensionProtocol,
  private val flexibleTypeDeserializer: FlexibleTypeDeserializer,
  private val expectedBinaryVersion: V,
  private val invalidBinaryVersion: V,
  stubVersion: Int
) : ClassFileDecompilers.Full() {
  private val stubBuilder = KonanMetadataStubBuilder(stubVersion, fileType, serializerProtocol, this::readFile)

  private val renderer = DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }

  protected abstract fun doReadFile(file: VirtualFile): FileWithMetadata?

  override fun accepts(file: VirtualFile) = file.fileType == fileType

  override fun getStubBuilder() = stubBuilder

  override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
    return createDecompiledFileViewProvider(manager, file, physical, this::buildDecompiledText)
  }

  private fun readFile(file: VirtualFile): FileWithMetadata? {
    if (!file.isValid) return null

    return try {
      doReadFile(file)
    }
    catch (e: IOException) {
      // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
      // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
      // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
      // it's not always allowed and also is likely to degrade performance
      null
    }
  }

  private fun buildDecompiledText(virtualFile: VirtualFile): DecompiledText {
    if (virtualFile.fileType != fileType) {
      error("Unexpected file type ${virtualFile.fileType}")
    }

    val file = readFile(virtualFile)

    return when (file) {
      null -> {
        createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion, invalidBinaryVersion)
      }
      is FileWithMetadata.Incompatible -> {
        createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion, file.version)
      }
      is FileWithMetadata.Compatible -> {
        decompiledText(file, targetPlatform, serializerProtocol, flexibleTypeDeserializer, renderer)
      }
    }
  }
}

sealed class FileWithMetadata {
  class Incompatible(val version: BinaryVersion) : FileWithMetadata()

  open class Compatible(
    val proto: KonanLinkData.LinkDataPackageFragment,
    serializerProtocol: SerializerExtensionProtocol
  ) : FileWithMetadata() {
    val nameResolver = NameResolverImpl(proto.stringTable, proto.nameTable)
    val packageFqName = FqName(nameResolver.getPackageFqName(proto.`package`.getExtension(serializerProtocol.packageFqName)))

    open val classesToDecompile: List<ProtoBuf.Class> =
      proto.classes.classesList.filter { proto ->
        val classId = nameResolver.getClassId(proto.fqName)
        !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
      }
  }
}

//todo: this function is extracted for KonanMetadataStubBuilder, that's the difference from Big Kotlin.
fun decompiledText(file: FileWithMetadata.Compatible, targetPlatform: TargetPlatform,
                   serializerProtocol: SerializerExtensionProtocol,
                   flexibleTypeDeserializer: FlexibleTypeDeserializer,
                   renderer: DescriptorRenderer): DecompiledText {
  val packageFqName = file.packageFqName
  val resolver = KonanMetadataDeserializerForDecompiler(
    packageFqName, file.proto, file.nameResolver,
    targetPlatform, serializerProtocol, flexibleTypeDeserializer
  )
  val declarations = arrayListOf<DeclarationDescriptor>()
  declarations.addAll(resolver.resolveDeclarationsInFacade(packageFqName))
  for (classProto in file.classesToDecompile) {
    val classId = file.nameResolver.getClassId(classProto.fqName)
    declarations.addIfNotNull(resolver.resolveTopLevelClass(classId))
  }
  return buildDecompiledText(packageFqName, declarations, renderer)
}

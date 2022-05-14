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

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.incremental.storage.StringCollectionExternalizer
import org.jetbrains.kotlin.incremental.storage.StringToLongMapExternalizer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.util.*

open class IncrementalJsCache(cachesDir: File) : IncrementalCacheCommon(cachesDir) {
    companion object {
        private val TRANSLATION_RESULT_MAP = "translation-result"
        private val LIBRARIES_PROTO = "libraries-proto"
        private val SOURCES_TO_CLASSES_FQNS = "sources-to-classes"
        private val INLINE_FUNCTIONS = "inline-functions"
        private val HEADER_FILE_NAME = "header.meta"
    }

    private val dirtySources = arrayListOf<File>()
    private val translationResults = registerMap(TranslationResultMap(TRANSLATION_RESULT_MAP.storageFile))
    private val librariesProtoMap = registerMap(LibrariesProtoMap(LIBRARIES_PROTO.storageFile))
    private val sourcesToClasses = registerMap(SourceToClassesMap(SOURCES_TO_CLASSES_FQNS.storageFile))
    private val inlineFunctions = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile))

    private val headerFile: File
        get() = File(cachesDir, HEADER_FILE_NAME)

    var header: ByteArray
        get() = headerFile.readBytes()
        set(value) {
            cachesDir.mkdirs()
            headerFile.writeBytes(value)
        }

    override fun markDirty(removedAndCompiledSources: List<File>) {
        dirtySources.addAll(removedAndCompiledSources)
    }

    fun compareAndUpdate(incrementalResults: IncrementalResultsConsumerImpl, changesCollector: ChangesCollector) {
        val translatedFiles = incrementalResults.packageParts

        dirtySources.forEach {
            if (it !in translatedFiles) {
                translationResults.remove(it, changesCollector)
                inlineFunctions.remove(it)
            }

            removeAllFromClassStorage(sourcesToClasses[it])
            sourcesToClasses.clearOutputsForSource(it)
        }
        dirtySources.clear()

        for ((srcFile, data) in translatedFiles) {
            val (binaryMetadata, binaryAst) = data

            val oldProtoView = translationResults.findProtoView(srcFile) ?: ProtoDataView()
            val newProtoView = ProtoDataView().apply { addDataFromFragment(binaryMetadata) }

            for ((classProto, nameResolver) in newProtoView.classes.values) {
                addToClassStorage(classProto, nameResolver, srcFile)
            }

            changesCollector.collectProtoChanges(oldProtoView, newProtoView)
            translationResults.put(srcFile, binaryMetadata, binaryAst)
        }

        for ((srcFile, inlineDeclarations) in incrementalResults.inlineFunctions) {
            inlineFunctions.process(srcFile, inlineDeclarations, changesCollector)
        }
    }

    fun compareLibraries(newLibs: Map<File, JsLibraryProtoMapValue>): LibrariesChangesEither {
        fun ByteArray.toLibraryProtoView(): ProtoDataView {
            val (_, library) = KotlinJavascriptSerializationUtil.readLibraryMetadata(this)
            return ProtoDataView().apply { addDataFrom(library) }
        }

        val oldLibs = librariesProtoMap.keys()
        for (oldLib in oldLibs) {
            if (oldLib !in newLibs) return LibrariesChangesEither.Unknown("Library was removed: $oldLib")
        }
        for (newLib in newLibs.keys) {
            if (newLib !in oldLibs) return LibrariesChangesEither.Unknown("Library was added: $newLib")
        }

        val changesCollector = ChangesCollector()
        // oldLibs == newLibs.keys at this point
        for (lib in oldLibs) {
            val oldLib = librariesProtoMap[lib]!!.modules
            val newLib = newLibs[lib]!!.modules

            for (module in oldLib.keys + newLib.keys) {
                val oldBytes = oldLib[module]
                val newBytes = newLib[module]

                if (Arrays.equals(oldBytes, newBytes)) continue

                val oldProtoView = oldBytes?.toLibraryProtoView() ?: ProtoDataView()
                val newProtoView = oldBytes?.toLibraryProtoView() ?: ProtoDataView()
                changesCollector.collectProtoChanges(oldProtoView, newProtoView)
            }
        }
        return LibrariesChangesEither.Known(changesCollector)
    }

    fun updateLibaries(libraries: Map<File, JsLibraryProtoMapValue>) {
        librariesProtoMap.clean()
        for ((file, lib) in libraries) {
            librariesProtoMap.put(file, lib)
        }
    }

    fun nonDirtyPackageParts(): Map<File, TranslationResultValue> =
            hashMapOf<File, TranslationResultValue>().apply {
                for (path in translationResults.keys()) {
                    val file = File(path)
                    if (file !in dirtySources) {
                        put(file, translationResults[path]!!)
                    }
                }
            }
}

sealed class LibrariesChangesEither {
    class Known(val changesCollector: ChangesCollector) : LibrariesChangesEither()
    class Unknown(val reason: String) : LibrariesChangesEither()
}

private class SourceToClassesMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {
    fun clearOutputsForSource(sourceFile: File) {
        remove(sourceFile.canonicalPath)
    }

    fun add(sourceFile: File, className: FqName) {
        storage.append(sourceFile.canonicalPath, className.asString())
    }

    operator fun get(sourceFile: File): Collection<FqName> =
            storage[sourceFile.canonicalPath].orEmpty().map { FqName(it) }

    override fun dumpValue(value: Collection<String>) = value.dumpCollection()

    private fun remove(path: String) {
        storage.remove(path)
    }
}

private object TranslationResultValueExternalizer : DataExternalizer<TranslationResultValue> {
    override fun save(output: DataOutput, value: TranslationResultValue) {
        output.writeInt(value.metadata.size)
        output.write(value.metadata)

        output.writeInt(value.binaryAst.size)
        output.write(value.binaryAst)
    }

    override fun read(input: DataInput): TranslationResultValue {
        val metadataSize = input.readInt()
        val metadata = ByteArray(metadataSize)
        input.readFully(metadata)

        val binaryAstSize = input.readInt()
        val binaryAst = ByteArray(binaryAstSize)
        input.readFully(binaryAst)

        return TranslationResultValue(metadata = metadata, binaryAst = binaryAst)
    }
}

private class TranslationResultMap(storageFile: File) : BasicStringMap<TranslationResultValue>(storageFile, TranslationResultValueExternalizer) {
    override fun dumpValue(value: TranslationResultValue): String =
            "Metadata: ${value.metadata.md5()}, Binary AST: ${value.binaryAst.md5()}"

    fun put(file: File, newMetadata: ByteArray, newBinaryAst: ByteArray) {
        storage[file.canonicalPath] = TranslationResultValue(metadata = newMetadata, binaryAst = newBinaryAst)
    }

    operator fun get(file: File): TranslationResultValue? =
            storage[file.canonicalPath]

    operator fun get(key: String): TranslationResultValue? =
            storage[key]

    fun keys(): Collection<String> =
            storage.keys

    fun findProtoView(file: File): ProtoDataView? {
        val bytes = get(file)?.metadata ?: return null
        return ProtoDataView().apply { addDataFromFragment(bytes) }
    }

    fun remove(file: File, changesCollector: ChangesCollector) {
        val bytes = get(file)?.metadata ?: error("Could not find proto for file $file")
        val protoView = ProtoDataView().apply { addDataFromFragment(bytes) }
        for (protoData in protoView.allProto) {
            changesCollector.collectProtoChanges(oldData = protoData, newData = null)
        }
        storage.remove(file.canonicalPath)
    }
}

private class InlineFunctionsMap(storageFile: File) : BasicStringMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
    fun process(srcFile: File, newMap: Map<String, Long>, changesCollector: ChangesCollector) {
        val key = srcFile.canonicalPath
        val oldMap = storage[key] ?: emptyMap()

        if (newMap.isNotEmpty()) {
            storage[key] = newMap
        }
        else {
            storage.remove(key)
        }

        for (fn in oldMap.keys + newMap.keys) {
            val fqNameSegments = fn.removePrefix("<get>").removePrefix("<set>").split(".")
            val fqName = FqName.fromSegments(fqNameSegments)
            changesCollector.collectMemberIfValueWasChanged(fqName.parent(), fqName.shortName().asString(), oldMap[fn], newMap[fn])
        }
    }

    fun remove(sourceFile: File) {
        storage.remove(sourceFile.canonicalPath)
    }

    override fun dumpValue(value: Map<String, Long>): String =
            value.dumpMap { java.lang.Long.toHexString(it) }
}


private object ModuleMapExternalizer : DataExternalizer<JsLibraryProtoMapValue> {
    override fun save(output: DataOutput, value: JsLibraryProtoMapValue) {
        val modules = value.modules
        output.writeInt(modules.size)

        for ((name, bytes) in modules) {
            output.writeUTF(name)
            output.write(bytes.size)
            output.write(bytes)
        }
    }

    override fun read(input: DataInput): JsLibraryProtoMapValue {
        val size = input.readInt()
        val modules = HashMap<String, ByteArray>()

        repeat(size) {
            val name = input.readUTF()
            val bytesSize = input.readInt()
            val bytes = ByteArray(bytesSize)
            input.readFully(bytes)
            modules[name] = bytes
        }

        return JsLibraryProtoMapValue(modules)
    }
}

class JsLibraryProtoMapValue(val modules: Map<String, ByteArray>)

private class LibrariesProtoMap(storageFile: File) : BasicStringMap<JsLibraryProtoMapValue>(storageFile, ModuleMapExternalizer) {
    override fun dumpValue(value: JsLibraryProtoMapValue): String =
            value.modules.dumpMap { "${it.md5()}" }

    fun put(file: File, value: JsLibraryProtoMapValue) {
        storage[file.canonicalPath] = value
    }

    operator fun get(file: File): JsLibraryProtoMapValue? =
        storage[file.canonicalPath]

    fun keys(): Set<File> =
        storage.keys.mapTo(HashSet()) { File(it) }
}
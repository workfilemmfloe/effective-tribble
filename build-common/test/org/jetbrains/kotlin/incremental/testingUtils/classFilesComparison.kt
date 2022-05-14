/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental.testingUtils

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.ExtensionRegistry
import org.jetbrains.kotlin.serialization.DebugProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.js.DebugJsProtoBuf
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.DebugJvmProtoBuf
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import org.junit.Assert
import org.junit.Assert.assertNotNull
import java.io.ByteArrayInputStream
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream
import kotlin.comparisons.compareBy

// Set this to true if you want to dump all bytecode (test will fail in this case)
private val DUMP_ALL = System.getProperty("comparison.dump.all") == "true"

fun assertEqualDirectories(expected: File, actual: File, forgiveExtraFiles: Boolean) {
    val pathsInExpected = getAllRelativePaths(expected)
    val pathsInActual = getAllRelativePaths(actual)

    val commonPaths = pathsInExpected.intersect(pathsInActual)
    val changedPaths = commonPaths
            .filter { DUMP_ALL || !Arrays.equals(File(expected, it).readBytes(), File(actual, it).readBytes()) }
            .sorted()

    val expectedString = getDirectoryString(expected, changedPaths)
    val actualString = getDirectoryString(actual, changedPaths)

    if (DUMP_ALL) {
        Assert.assertEquals(expectedString, actualString + " ")
    }

    if (forgiveExtraFiles) {
        // If compilation fails, output may be different for full rebuild and partial make. Parsing output (directory string) for simplicity.
        if (changedPaths.isEmpty()) {
            val expectedListingLines = expectedString.split('\n').toList()
            val actualListingLines = actualString.split('\n').toList()
            if (actualListingLines.containsAll(expectedListingLines)) {
                return
            }
        }
    }

    Assert.assertEquals(expectedString, actualString)
}

private fun File.checksumString(): String {
    val crc32 = CRC32()
    crc32.update(this.readBytes())
    return java.lang.Long.toHexString(crc32.value)
}

private fun getDirectoryString(dir: File, interestingPaths: List<String>): String {
    val buf = StringBuilder()
    val p = Printer(buf)


    fun addDirContent(dir: File) {
        p.pushIndent()

        val listFiles = dir.listFiles()
        assertNotNull("$dir does not exist", listFiles)

        val children = listFiles!!.sortedWith(compareBy({ it.isDirectory }, { it.name }))
        for (child in children) {
            if (child.isDirectory) {
                if ((child.list()?.isNotEmpty() ?: false)) {
                    p.println(child.name)
                    addDirContent(child)
                }
            }
            else {
                p.println(child.name, " ", child.checksumString())
            }
        }

        p.popIndent()
    }


    p.println(".")
    addDirContent(dir)

    for (path in interestingPaths) {
        p.println("================", path, "================")
        p.println(fileToStringRepresentation(File(dir, path)))
        p.println()
        p.println()
    }

    return buf.toString()
}

private fun getAllRelativePaths(dir: File): Set<String> {
    val result = HashSet<String>()
    FileUtil.processFilesRecursively(dir) {
        if (it!!.isFile) {
            result.add(FileUtil.getRelativePath(dir, it)!!)
        }

        true
    }

    return result
}

private fun classFileToString(classFile: File): String {
    val out = StringWriter()

    val traceVisitor = TraceClassVisitor(PrintWriter(out))
    ClassReader(classFile.readBytes()).accept(traceVisitor, 0)

    val classHeader = LocalFileKotlinClass.create(classFile)?.classHeader

    val annotationDataEncoded = classHeader?.data
    if (annotationDataEncoded != null) {
        ByteArrayInputStream(BitEncoding.decodeBytes(annotationDataEncoded)).use {
            input ->

            out.write("\n------ string table types proto -----\n${DebugJvmProtoBuf.StringTableTypes.parseDelimitedFrom(input)}")

            if (!classHeader.metadataVersion.isCompatible()) {
                error("Incompatible class ($classHeader): $classFile")
            }

            when (classHeader.kind) {
                KotlinClassHeader.Kind.FILE_FACADE ->
                    out.write("\n------ file facade proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")
                KotlinClassHeader.Kind.CLASS ->
                    out.write("\n------ class proto -----\n${DebugProtoBuf.Class.parseFrom(input, getExtensionRegistry())}")
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART ->
                    out.write("\n------ multi-file part proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")
                else -> throw IllegalStateException()
            }
        }
    }

    return out.toString()
}

private fun getExtensionRegistry(): ExtensionRegistry {
    val registry = ExtensionRegistry.newInstance()!!
    DebugJvmProtoBuf.registerAllExtensions(registry)
    return registry
}

private fun fileToStringRepresentation(file: File): String {
    return when {
        file.name.endsWith(".meta.js") -> {
            val out = StringWriter()
            val p = Printer(out)

            val modules = KotlinJavascriptMetadataUtils.loadMetadata(file)
            for (module in modules) {
                p.println(">>> Module ${module.moduleName} (version ${module.version.toInteger()}) <<<")
                val (header, library) = GZIPInputStream(ByteArrayInputStream(module.body)).use { stream ->
                    DebugJsProtoBuf.Header.parseDelimitedFrom(stream, JsSerializerProtocol.extensionRegistry) to
                    DebugJsProtoBuf.Library.parseFrom(stream, JsSerializerProtocol.extensionRegistry)
                }
                p.println("> Header of ${module.moduleName}")
                p.println(header.toString())

                p.println("> Library of ${module.moduleName}")
                library.packageFragmentList.forEach { fragment ->
                    val nameResolver = DebugNameResolverImpl(fragment.strings, fragment.qualifiedNames)

                    fragment.class_List.forEach { classProto ->
                        p.println("Class id ${classProto.fqName}->${nameResolver.getClassId(classProto.fqName)}")
                    }
                }
                p.println(library.toString())
                p.println()
            }
            out.toString()
        }
        file.name.endsWith(".kjsm") -> {
            ""
        }
        file.name.endsWith(".class") -> {
            classFileToString(file)
        }
        else -> {
            file.readText()
        }
    }
}


class DebugNameResolverImpl(
        private val strings: DebugProtoBuf.StringTable,
        private val qualifiedNames: DebugProtoBuf.QualifiedNameTable
) : NameResolver {
    override fun getString(index: Int): String = strings.getString(index)

    override fun getName(index: Int) = Name.guessByFirstCharacter(strings.getString(index))

    override fun getClassId(index: Int): ClassId {
        val (packageFqNameSegments, relativeClassNameSegments, isLocal) = traverseIds(index)
        return ClassId(FqName.fromSegments(packageFqNameSegments), FqName.fromSegments(relativeClassNameSegments), isLocal)
    }

    fun getPackageFqName(index: Int): FqName {
        val packageNameSegments = traverseIds(index).first
        return FqName.fromSegments(packageNameSegments)
    }

    private fun traverseIds(startingIndex: Int): Triple<List<String>, List<String>, Boolean> {
        var index = startingIndex
        val packageNameSegments = LinkedList<String>()
        val relativeClassNameSegments = LinkedList<String>()
        var local = false

        while (index != -1) {
            val proto = qualifiedNames.getQualifiedName(index)
            val shortName = strings.getString(proto.shortName)
            when (proto.kind!!) {
                ProtoBuf.QualifiedNameTable.QualifiedName.Kind.CLASS -> relativeClassNameSegments.addFirst(shortName)
                ProtoBuf.QualifiedNameTable.QualifiedName.Kind.PACKAGE -> packageNameSegments.addFirst(shortName)
                ProtoBuf.QualifiedNameTable.QualifiedName.Kind.LOCAL -> {
                    relativeClassNameSegments.addFirst(shortName)
                    local = true
                }
            }

            index = proto.parentQualifiedName
        }
        return Triple(packageNameSegments, relativeClassNameSegments, local)
    }
}

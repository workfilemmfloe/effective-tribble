/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.CONTENT_ROOTS
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.measureNanoTime


private fun NodeList.toList(): List<Node> {
    val list = mutableListOf<Node>()
    for (index in 0 until this.length) {
        list += item(index)
    }
    return list
}

private val Node.childNodesList get() = childNodes.toList()

private const val FAIL_FAST = true
private const val DUMP_FIR = true

private const val FIR_DUMP_PATH = "tmp/firDump"
private const val FIR_LOGS_PATH = "tmp/fir-logs"

private data class ModuleData(val name: String, val classpath: List<File>, val sources: List<File>, val javaSourceRoots: List<File>)

private const val PASSES = 1

class FirResolveModularizedTotalKotlinTest : KtUsefulTestCase() {


    private lateinit var bench: FirResolveBench

    private fun runAnalysis(moduleData: ModuleData, environment: KotlinCoreEnvironment) {
        val project = environment.project
        val ktFiles = environment.getSourceFiles()

        val scope = ProjectScope.getContentScope(project)
        val session = createSession(environment, scope)
        val builder = RawFirBuilder(session, stubMode = false)

        val totalTransformer = FirTotalResolveTransformer()
        val firFiles = ktFiles.toList().mapNotNull {
            var firFile: FirFile? = null
            val time = measureNanoTime {
                firFile = builder.buildFirFile(it)
                (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile!!)
            }
            bench.countBuilder(builder, time)
            firFile
        }.toList()


        println("Raw FIR up, files: ${firFiles.size}")

        bench.processFiles(firFiles, totalTransformer.transformers)

        dumpFir(moduleData, firFiles)
    }

    private fun dumpFir(moduleData: ModuleData, firFiles: List<FirFile>) {
        if (!DUMP_FIR) return
        val dumpRoot = File(FIR_DUMP_PATH).resolve(moduleData.name)
        firFiles.forEach {
            val directory = it.packageFqName.pathSegments().fold(dumpRoot) { file, name -> file.resolve(name.asString()) }
            directory.mkdirs()
            directory.resolve(it.name + ".fir").writeText(it.render())
        }
    }

    private fun processModule(moduleData: ModuleData) {
        val configurationKind = ConfigurationKind.ALL
        val testJdkKind = TestJdkKind.FULL_JDK


        val disposable = Disposer.newDisposable()

        val configuration =
            KotlinTestUtils.newConfiguration(configurationKind, testJdkKind, moduleData.classpath, moduleData.javaSourceRoots)

        configuration.put(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING, true)
        configuration.addAll(
            CONTENT_ROOTS,
            moduleData.sources.filter { it.extension == "kt" }.map { KotlinSourceRoot(it.absolutePath, false) })
        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        Extensions.getArea(environment.project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)

        runAnalysis(moduleData, environment)

        Disposer.dispose(disposable)
    }

    private fun loadModule(file: File): ModuleData {

        val factory = DocumentBuilderFactory.newInstance()
        factory.isIgnoringComments = true
        factory.isIgnoringElementContentWhitespace = true
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(file)
        val moduleElement = document.childNodes.item(0).childNodesList.first { it.nodeType == Node.ELEMENT_NODE }
        val moduleName = moduleElement.attributes.getNamedItem("name").nodeValue
        val javaSourceRoots = mutableListOf<File>()
        val classpath = mutableListOf<File>()
        val sources = mutableListOf<File>()

        for (index in 0 until moduleElement.childNodes.length) {
            val item = moduleElement.childNodes.item(index)

            if (item.nodeName == "classpath") {
                classpath += File(item.attributes.getNamedItem("path").nodeValue)
            }
            if (item.nodeName == "javaSourceRoots") {
                javaSourceRoots += File(item.attributes.getNamedItem("path").nodeValue)
            }
            if (item.nodeName == "sources") {
                sources += File(item.attributes.getNamedItem("path").nodeValue)
            }
        }

        return ModuleData(moduleName, classpath, sources, javaSourceRoots)
    }


    private fun runTestOnce(pass: Int) {
        val testDataPath = "/Users/jetbrains/jps"
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val modules =
            root.listFiles().sortedBy { it.lastModified() }.map { loadModule(it) }


        for (module in modules.progress(step = 0.0) { "Analyzing ${it.name}" }) {
            processModule(module)
            if (bench.hasFiles && FAIL_FAST) {
                break
            }
        }

        bench.report(System.out, errorTypeReports = false)

        saveReport()
        if (FAIL_FAST) {
            bench.throwFailure()
        }
    }

    fun saveReport() {
        val format = SimpleDateFormat("yyyy-MM-dd__hh-mm")
        val logDir = File(FIR_LOGS_PATH)
        logDir.mkdirs()
        PrintStream(logDir.resolve("report-${format.format(Date())}.log").outputStream()).use { stream ->
            bench.report(stream)
        }
    }

    fun testTotalKotlin() {
        for (i in 0 until PASSES) {
            println("Pass $i")

            bench = FirResolveBench(withProgress = false)
            runTestOnce(i)
        }
    }
}
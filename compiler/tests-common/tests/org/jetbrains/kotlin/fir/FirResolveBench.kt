/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.fir

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.concurrent.tcReg
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirResolveStage
import org.jetbrains.kotlin.fir.resolve.transformers.FirStagesTransformerFactory
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.PrintStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime


fun checkFirProvidersConsistency(firFiles: List<FirFile>) {
    for ((session, files) in firFiles.groupBy { it.session }) {
        val provider = session.service<FirProvider>() as FirProviderImpl
        provider.ensureConsistent(files)
    }
}

private data class FailureInfo(val stageClass: KClass<*>, val throwable: Throwable, val file: String)
private data class ErrorTypeReport(val report: String, var count: Int = 0)


private val threadLocalTransformer = ThreadLocal<FirTransformer<Nothing?>>()


class FirResolveBench(val withProgress: Boolean) {

    val summaryTimePerStage = mutableMapOf<KClass<*>, Long>()
    val realTimePerStage = mutableMapOf<KClass<*>, Long>()
    var resolvedTypes = 0
    var errorTypes = 0
    var unresolvedTypes = 0
    var errorFunctionCallTypes = 0
    var errorQualifiedAccessTypes = 0
    var implicitTypes = 0
    var fileCount = 0
    var totalLines = 0

    var parallelThreads = Runtime.getRuntime().availableProcessors()

    var isParallel = true

    val singleThreadPool = Executors.newSingleThreadExecutor()
    val pool by lazy { Executors.newFixedThreadPool(parallelThreads) }


    private val fails = mutableListOf<FailureInfo>()
    val hasFiles get() = fails.isNotEmpty()

    private val errorTypesReports = mutableMapOf<String, ErrorTypeReport>()

    fun countBuilder(builder: RawFirBuilder, time: Long) {
        summaryTimePerStage.merge(builder::class, time) { a, b -> a + b }
        realTimePerStage.merge(builder::class, time) { a, b -> a + b }
    }

    private fun runBuilder(builder: RawFirBuilder, ktFiles: List<KtFile>): List<FirFile> {
        return ktFiles.map { file ->
            var firFile: FirFile? = null
            val time = measureNanoTime {
                firFile = builder.buildFirFile(file)
                (builder.session.service<FirProvider>() as FirProviderImpl).recordFile(firFile!!)
            }
            summaryTimePerStage.merge(builder::class, time) { a, b -> a + b }
            firFile!!
        }
    }

    private fun runBuilderConcurrently(builder: RawFirBuilder, ktFiles: List<KtFile>): List<FirFile> {
        return ktFiles.map { file ->
            pool.submit(Callable {
                var firFile: FirFile? = null
                val time = measureNanoTime {
                    firFile = builder.buildFirFile(file)
                    (builder.session.service<FirProvider>() as FirProviderImpl).recordFile(firFile!!)
                }
                summaryTimePerStage.merge(builder::class, time) { a, b -> a + b }
                firFile!!
            })
        }.map { it.get() }
    }

    fun buildFiles(
        builder: RawFirBuilder,
        ktFiles: List<KtFile>
    ): List<FirFile> {
        val (result, time) = measureNanoTimeWithResult {
            if (isParallel)
                runBuilderConcurrently(builder, ktFiles)
            else
                runBuilder(builder, ktFiles)
        }
        realTimePerStage.merge(builder::class, time) { a, b -> a + b }
        ktFiles.forEach {
            totalLines += it.text.lines().count()
        }
        return result
    }


    private fun runStage(stage: FirResolveStage, firFiles: List<FirFile>) {
        val firFileSequence = if (withProgress) firFiles.progress("   ~ ") else firFiles.asSequence()
        val transformer = stage.createTransformer()
        for (firFile in firFileSequence) {
            var fail = false
            val time = measureNanoTime {
                try {
                    transformer.transformFile(firFile, null)
                } catch (e: Throwable) {
                    val ktFile = firFile.psi as KtFile
                    println("Fail in file: ${ktFile.virtualFilePath}")
                    fail = true
                    fails += FailureInfo(stage::class, e, ktFile.virtualFilePath)
                    //println(ktFile.text)
                    //throw e
                }
            }
            if (!fail) {
                summaryTimePerStage.merge(stage::class, time) { a, b -> a + b }
            }
        }
    }

    private fun runStageConcurrently(stage: FirResolveStage, firFiles: List<FirFile>) {
        require(stage.isParallel)
        val transformerLocal = object : ThreadLocal<FirTransformer<Nothing?>>() {
            override fun initialValue(): FirTransformer<Nothing?> {
                return stage.createTransformer()
            }
        }
        val tasks = (firFiles).shuffled().map { firFile ->
            firFile to pool.submit {
                var fail = false
                val time = measureNanoTime {
                    try {
                        transformerLocal.get().transformFile(firFile, null)
                    } catch (e: Throwable) {
                        val ktFile = firFile.psi as KtFile
                        println("Fail in file: ${ktFile.virtualFilePath}")
                        fail = true
                        fails += FailureInfo(stage::class, e, ktFile.virtualFilePath)
                        //println(ktFile.text)
                        //throw e
                    }
                }
                if (!fail) {
                    summaryTimePerStage.merge(stage::class, time) { a, b -> a + b }
                }
            }

        }
        val tasksSequence = if (withProgress) tasks.progress("   ~ ") else tasks.asSequence()
        tasksSequence.forEach { (_, future) ->
            future.get()
        }
    }

    fun processFiles(
        firFiles: List<FirFile>,
        factory: FirStagesTransformerFactory
    ) {
        fileCount += firFiles.size
        try {
            for ((stageNum, stage) in factory.resolveStages.withIndex()) {
                val usedThreads = if (stage.isParallel && isParallel) parallelThreads else 1
                println("Starting stage #$stageNum, $stage, isParallel = ${stage.isParallel}, using threads: $usedThreads")

                val realTime = measureNanoTime {

                    if (stage.isParallel && isParallel)
                        runStageConcurrently(stage, firFiles)
                    else
                        runStage(stage, firFiles)
                }


                realTimePerStage[stage::class] = realTime
                //totalLength += StringBuilder().apply { FirRenderer(this).visitFile(firFile) }.length
                checkFirProvidersConsistency(firFiles)
            }

            if (fails.none()) {
                println("SUCCESS!")
            } else {
                println("ERROR!")
            }
        } finally {


            val fileDocumentManager = FileDocumentManager.getInstance()

            firFiles.forEach {
                it.accept(object : FirVisitorVoid() {

                    fun reportProblem(problem: String, psi: PsiElement) {
                        val document = try {
                            fileDocumentManager.getDocument(psi.containingFile.virtualFile)
                        } catch (t: Throwable) {
                            throw Exception("for file ${psi.containingFile}", t)
                        }
                        val line = (document?.getLineNumber(psi.startOffset) ?: 0)
                        val char = psi.startOffset - (document?.getLineStartOffset(line) ?: 0)
                        val report = "e: ${psi.containingFile?.virtualFile?.path}: (${line + 1}:$char): $problem"
                        errorTypesReports.getOrPut(problem) { ErrorTypeReport(report) }.count++
                    }

                    override fun visitElement(element: FirElement) {
                        element.acceptChildren(this)
                    }

                    override fun visitFunctionCall(functionCall: FirFunctionCall) {
                        val typeRef = functionCall.typeRef
                        if (typeRef is FirResolvedTypeRef) {
                            val type = typeRef.type
                            if (type is ConeKotlinErrorType) {
                                errorFunctionCallTypes++
                            }
                        }

                        super.visitFunctionCall(functionCall)
                    }

                    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
                        val typeRef = qualifiedAccessExpression.typeRef
                        if (typeRef is FirResolvedTypeRef) {
                            val type = typeRef.type
                            if (type is ConeKotlinErrorType) {
                                errorQualifiedAccessTypes++
                            }
                        }

                        super.visitQualifiedAccessExpression(qualifiedAccessExpression)
                    }

                    override fun visitTypeRef(typeRef: FirTypeRef) {
                        unresolvedTypes++

                        if (typeRef.psi != null) {
                            val psi = typeRef.psi!!
                            val problem = "${typeRef::class.simpleName}: ${typeRef.render()}"
                            reportProblem(problem, psi)
                        }
                    }

                    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
                        if (implicitTypeRef is FirResolvedTypeRef) {
                            visitResolvedTypeRef(implicitTypeRef)
                        } else {
                            visitTypeRef(implicitTypeRef)
                        }
                    }

                    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                        resolvedTypes++
                        val type = resolvedTypeRef.type
                        if (type is ConeKotlinErrorType || type is ConeClassErrorType) {
                            if (resolvedTypeRef.psi == null) {
                                implicitTypes++
                            } else {
                                errorTypes++
                                val psi = resolvedTypeRef.psi!!
                                val problem = "${resolvedTypeRef::class.simpleName} -> ${type::class.simpleName}: ${type.render()}"
                                reportProblem(problem, psi)
                            }
                        }
                    }
                })
            }
        }


    }

    fun throwFailure() {
        if (fails.any()) {
            val (transformerClass, failure, file) = fails.first()
            throw AssertionError("Failures detected in ${transformerClass.simpleName}, file: $file", failure)
        }
    }

    fun report(stream: PrintStream, errorTypeReports: Boolean = true) {

        if (errorTypeReports)
            errorTypesReports.values.sortedByDescending { it.count }.forEach {
                stream.print("${it.count}:")
                stream.println(it.report)
            }

        infix fun Int.percentOf(other: Int): String {
            return String.format("%.1f%%", this * 100.0 / other)
        }

        val totalTypes = unresolvedTypes + resolvedTypes
        stream.println("UNRESOLVED (UNTOUCHED) IMPLICIT TYPES: $unresolvedTypes (${unresolvedTypes percentOf totalTypes})")
        stream.println("RESOLVED TYPES: $resolvedTypes (${resolvedTypes percentOf totalTypes})")
        val goodTypes = resolvedTypes - errorTypes - implicitTypes
        stream.println("CORRECTLY RESOLVED TYPES: $goodTypes (${goodTypes percentOf resolvedTypes} of resolved)")
        stream.println("ERRONEOUSLY RESOLVED TYPES: $errorTypes (${errorTypes percentOf resolvedTypes} of resolved)")
        stream.println("   - unresolved calls: $errorFunctionCallTypes")
        stream.println("   - unresolved q.accesses: $errorQualifiedAccessTypes")
        stream.println("ERRONEOUSLY RESOLVED IMPLICIT TYPES: $implicitTypes (${implicitTypes percentOf resolvedTypes} of resolved)")
        stream.println("UNIQUE ERROR TYPES: ${errorTypesReports.size}")
        for (c in tcReg) {
            stream.println(c.stats())
        }


        var totalTime = 0L
        var totalSummaryTime = 0L


        realTimePerStage.forEach { (stageClass, time) ->
            val counter = fileCount - fails.count { it.stageClass == stageClass }
            val summaryTime = summaryTimePerStage[stageClass]!!
            stream.println("${stageClass.simpleName}, TIME: ${time * 1e-6} ms, TIME/FILE: ${(time / counter) * 1e-6} ms, S-TIME/TIME: ${summaryTime / (time * 1.0)}, S-TIME: ${summaryTime * 1e-6} ms, S-TIME/FILE: ${summaryTime / counter * 1e-6} ms, FILES: OK/E/T $counter/${fileCount - counter}/$fileCount")
            totalTime += time
            totalSummaryTime += summaryTime
        }

        stream.println("Total, TIME: ${totalTime * 1e-6} ms, TIME PER FILE: ${(totalTime / fileCount) * 1e-6} ms, S-TIME/TIME: ${totalSummaryTime / (totalTime * 1.0)}, S-TIME: ${totalSummaryTime * 1e-6} ms, S-TIME/FILE: ${totalSummaryTime / fileCount * 1e-6} ms")
        stream.println("  ${totalLines / (totalTime * 1e-9)} Line/s")
    }
}

fun doFirResolveTestBench(
    firFiles: List<FirFile>,
    factory: FirStagesTransformerFactory,
    gc: Boolean = true,
    withProgress: Boolean = false
) {

    if (gc) {
        System.gc()
    }

    val bench = FirResolveBench(withProgress)
    bench.parallelThreads = 1
    bench.processFiles(firFiles, factory)
    bench.report(System.out)
    bench.throwFailure()
}


fun <T> Collection<T>.progress(label: String, step: Double = 0.1): Sequence<T> {
    return progress(step) { label }
}

fun <T> Collection<T>.progress(step: Double = 0.1, computeLabel: (T) -> String): Sequence<T> {
    val intStep = max(1, (this.size * step).toInt())
    var progress = 0
    val startTime = System.currentTimeMillis()

    fun Long.formatTime(): String {
        return when {
            this < 1000 -> "${this}ms"
            this < 60 * 1000 -> "${this / 1000}s ${this % 1000}ms"
            else -> "${this / (60 * 1000)}m ${this % (60 * 1000) / 1000}s ${this % (60 * 1000) % 1000}ms"
        }
    }

    return asSequence().onEach {
        if (progress % intStep == 0) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - startTime

            val eta = if (progress > 0) ((elapsed / progress * 1.0) * (this.size - progress)).toLong().formatTime() else "Unknown"
            println("${computeLabel(it)}: ${progress * 100 / size}% ($progress/${this.size}), ETA: $eta, Elapsed: ${elapsed.formatTime()}")
        }
        progress++
    }
}

private inline fun <T> measureNanoTimeWithResult(crossinline block: () -> T): Pair<T, Long> {
    var result: Any? = null
    val time = measureNanoTime {
        result = block()
    }
    return result as T to time
}
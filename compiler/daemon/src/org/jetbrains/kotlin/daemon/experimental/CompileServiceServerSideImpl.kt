/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.daemon.experimental

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import io.ktor.network.sockets.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.LazyClasspathWatcher
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.WallAndThreadAndMemoryTotalProfiler
import org.jetbrains.kotlin.daemon.common.WallAndThreadTotalProfiler
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import org.jetbrains.kotlin.daemon.common.impls.*
import org.jetbrains.kotlin.daemon.nowSeconds
import org.jetbrains.kotlin.daemon.report.RemoteICReporter
import org.jetbrains.kotlin.daemon.report.experimental.CompileServicesFacadeMessageCollector
import org.jetbrains.kotlin.daemon.report.experimental.DaemonMessageReporterAsync
import org.jetbrains.kotlin.daemon.report.experimental.RemoteICReporterAsync
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryAndroid
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJvm
import org.jetbrains.kotlin.incremental.parsing.classesFqNames
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.progress.experimental.CompilationCanceledStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.rmi.RemoteException
import java.security.PrivateKey
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.read
import kotlin.concurrent.schedule
import kotlin.concurrent.write

interface EventManager {
    suspend fun onCompilationFinished(f: suspend () -> Unit)
}

private class EventManagerImpl : EventManager {
    private val onCompilationFinished = arrayListOf<suspend () -> Unit>()

    @Throws(RemoteException::class)
    override suspend fun onCompilationFinished(f: suspend () -> Unit) {
        onCompilationFinished.add(f)
    }

    suspend fun fireCompilationFinished() {
        onCompilationFinished.forEach { it() }
    }
}

class CompileServiceServerSideImpl(
    override val serverSocketWithPort: ServerSocketWrapper,
    val compiler: CompilerSelector,
    val compilerId: CompilerId,
    val daemonOptions: DaemonOptions,
    val daemonJVMOptions: DaemonJVMOptions,
    val port: Int,
    val timer: Timer,
    val onShutdown: () -> Unit
) : CompileServiceServerSide {

    override val serverPort: Int
        get() = serverSocketWithPort.port

    override val clients = hashMapOf<Socket, Server.ClientInfo>()

    object KeepAliveServer : Server<ServerBase> {
        override val serverSocketWithPort = findCallbackServerSocket()
        override val clients = hashMapOf<Socket, Server.ClientInfo>()

    }

    override suspend fun securityCheck(clientInputChannel: ByteReadChannelWrapper): Boolean = runWithTimeout {
        getSignatureAndVerify(clientInputChannel, securityData.token, securityData.publicKey)
    } ?: false

    override suspend fun serverHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger): Boolean {
        return tryAcquireHandshakeMessage(input, log) && trySendHandshakeMessage(output, log)
    }

    interface CompileServiceTask
    interface CompileServiceTaskWithResult : CompileServiceTask

    open class ExclusiveTask(val completed: CompletableDeferred<Boolean>, val shutdownAction: suspend () -> Any) : CompileServiceTask
    open class ShutdownTaskWithResult(val result: CompletableDeferred<Any>, shutdownAction: suspend () -> Any) :
        ExclusiveTask(CompletableDeferred(), shutdownAction), CompileServiceTaskWithResult

    open class OrdinaryTask(val completed: CompletableDeferred<Boolean>, val action: suspend () -> Any) : CompileServiceTask
    class OrdinaryTaskWithResult(val result: CompletableDeferred<Any>, action: suspend () -> Any) :
        OrdinaryTask(CompletableDeferred(), action),
        CompileServiceTaskWithResult

    class TaskFinished(val taskId: Int) : CompileServiceTask
    class ExclusiveTaskFinished : CompileServiceTask

    var isWriteLocked = false
    var readLocksCount = 0
    val queriesActor = actor<CompileServiceTask>(capacity = Channel.UNLIMITED) {
        var currentTaskId = 0
        var shutdownTask: ExclusiveTask? = null
        val activeTaskIds = arrayListOf<Int>()
        val waitingTasks = arrayListOf<CompileServiceTask>()
        fun tryInvokeShutdown(reason: String) {
            println("tryInvokeShutdown | reason = $reason | tasks = ${activeTaskIds} | shutdownTask = ${shutdownTask != null}")
            if (activeTaskIds.isEmpty()) {
                shutdownTask?.let { task ->
                    isWriteLocked = true
                    async {
                        val res = task.shutdownAction()
                        task.completed.complete(true)
                        if (task is ShutdownTaskWithResult) {
                            task.result.complete(res)
                        }
                        channel.send(ExclusiveTaskFinished())
                    }
                }
            }
        }
        consumeEach { task ->
            when (task) {
                is ExclusiveTask -> {
                    if (shutdownTask == null) {
                        shutdownTask = task
                        tryInvokeShutdown("ExclusiveTask")
                    } else {
                        waitingTasks.add(task)
                    }
                }
                is OrdinaryTask -> {
                    if (shutdownTask == null) {
                        val id = currentTaskId++
                        activeTaskIds.add(id)
                        readLocksCount++
                        async {
                            val res = task.action()
                            if (task is OrdinaryTaskWithResult) {
                                task.result.complete(res)
                            }
                            task.completed.complete(true)
                            channel.send(TaskFinished(id))
                        }
                    } else {
                        waitingTasks.add(task)
                    }
                }
                is TaskFinished -> {
                    log.info("TaskFinished!!!")
                    activeTaskIds.remove(task.taskId)
                    readLocksCount--
                    tryInvokeShutdown("TaskFinished")
                }
                is ExclusiveTaskFinished -> {
                    shutdownTask = null
                    isWriteLocked = false
                    waitingTasks.forEach {
                        channel.send(it)
                    }
                    waitingTasks.clear()
                }
            }
        }
    }

    constructor(
        serverSocket: ServerSocketWrapper,
        compilerId: CompilerId,
        daemonOptions: DaemonOptions,
        daemonJVMOptions: DaemonJVMOptions,
        port: Int,
        timer: Timer,
        onShutdown: () -> Unit
    ) : this(
        serverSocket,
        CompilerSelector.getDefault(),
        compilerId,
        daemonOptions,
        daemonJVMOptions,
        port,
        timer,
        onShutdown
    )

    private val log by lazy { Logger.getLogger("compiler") }

    init {
        log.info("Running OLD server (port = $port)")
        System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")
    }

    // wrapped in a class to encapsulate alive check logic
    private class ClientOrSessionProxy<out T : Any>(
        val aliveFlagPath: String?,
        val data: T? = null,
        private var disposable: Disposable? = null
    ) {
        val isAlive: Boolean
            get() = aliveFlagPath?.let { File(it).exists() } ?: true // assuming that if no file was given, the client is alive

        fun dispose() {
            disposable?.let {
                Disposer.dispose(it)
                disposable = null
            }
        }
    }

    private val compilationsCounter = AtomicInteger(0)

    private val classpathWatcher = LazyClasspathWatcher(compilerId.compilerClasspath)

    enum class Aliveness {
        // !!! ordering of values is used in state comparison
        Dying,
        LastSession, Alive
    }

    private class SessionsContainer {

        private val lock = ReentrantReadWriteLock()
        private val sessions: MutableMap<Int, ClientOrSessionProxy<Any>> = hashMapOf()
        private val sessionsIdCounter = AtomicInteger(0)

        val lastSessionId get() = sessionsIdCounter.get()

        fun <T : Any> leaseSession(session: ClientOrSessionProxy<T>): Int = lock.write {
            val newId = getValidId(sessionsIdCounter) {
                it != CompileService.NO_SESSION && !sessions.containsKey(it)
            }
            sessions.put(newId, session)
            newId
        }

        fun isEmpty(): Boolean = lock.read { sessions.isEmpty() }

        operator fun get(sessionId: Int) = lock.read { sessions[sessionId] }

        fun remove(sessionId: Int): Boolean = lock.write {
            sessions.remove(sessionId)?.apply { dispose() } != null
        }

        fun cleanDead(): Boolean {
            var anyDead = false
            lock.read {
                val toRemove = sessions.filterValues { !it.isAlive }
                if (toRemove.isNotEmpty()) {
                    anyDead = true
                    lock.write {
                        toRemove.forEach { sessions.remove(it.key)?.dispose() }
                    }
                }
            }
            return anyDead
        }
    }


    // TODO: encapsulate operations on state here
    private val state = object {

        private val clientsLock = ReentrantReadWriteLock()
        private val clientProxies: MutableSet<ClientOrSessionProxy<Any>> = hashSetOf()

        val sessions = SessionsContainer()

        val delayedShutdownQueued = AtomicBoolean(false)

        var alive = AtomicInteger(Aliveness.Alive.ordinal)

        val aliveClientsCount: Int get() = clientProxies.size

        private val _clientsCounter = AtomicInteger(0)

        val clientsCounter get() = _clientsCounter.get()

        fun addClient(aliveFlagPath: String?) {
            clientsLock.write {
                _clientsCounter.incrementAndGet()
                clientProxies.add(ClientOrSessionProxy(aliveFlagPath))
            }
        }

        fun getClientsFlagPaths(): List<String> = clientsLock.read {
            clientProxies.mapNotNull { it.aliveFlagPath }
        }

        fun cleanDeadClients(): Boolean =
            clientProxies.cleanMatching(clientsLock, { !it.isAlive }, { if (clientProxies.remove(it)) it.dispose() })
    }

    private fun Int.toAlivenessName(): String =
        try {
            Aliveness.values()[this].name
        } catch (_: Throwable) {
            "invalid($this)"
        }

    private inline fun <T> Iterable<T>.cleanMatching(
        lock: ReentrantReadWriteLock,
        crossinline pred: (T) -> Boolean,
        crossinline clean: (T) -> Unit
    ): Boolean {
        var anyDead = false
        lock.read {
            val toRemove = filter(pred)
            if (toRemove.isNotEmpty()) {
                anyDead = true
                lock.write {
                    toRemove.forEach(clean)
                }
            }
        }
        return anyDead
    }

    @Volatile
    private var _lastUsedSeconds = nowSeconds()
    val lastUsedSeconds: Long
        get() = (if (readLocksCount > 1 || isWriteLocked) nowSeconds() else _lastUsedSeconds).also {
            log.info("lastUsedSeconds .. isReadLockedCNT : $readLocksCount , isWriteLocked : $isWriteLocked")
        }

    private var runFile: File
    private var securityData: SecurityData

    init {
        val runFileDir = File(daemonOptions.runFilesPathOrDefault)
        runFileDir.mkdirs()
        log.info("port.toString() = $port | serverSocketWithPort = $serverSocketWithPort")
        runFile = File(
            runFileDir,
            makeRunFilenameString(
                timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString(),
                port = port.toString()
            )
        )
        try {
            if (!runFile.createNewFile()) throw Exception("createNewFile returned false")
        } catch (e: Throwable) {
            throw IllegalStateException("Unable to create runServer file '${runFile.absolutePath}'", e)
        }
        var privateKey: PrivateKey?
        securityData = generateKeysAndToken()
        runFile.outputStream().use {
            sendTokenKeyPair(it, securityData.token, securityData.privateKey)
        }
        runFile.deleteOnExit()
    }

    // RMI-exposed API

    override suspend fun getDaemonInfo(): CompileService.CallResult<String> =
        ifAlive(minAliveness = Aliveness.Dying, info = "getDaemonInfo") {
            CompileService.CallResult.Good("Kotlin daemon on socketPort $port")
        }

    override suspend fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> = ifAlive(info = "getDaemonOptions") {
        CompileService.CallResult.Good(daemonOptions)
    }

    override suspend fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> = ifAlive(info = "getDaemonJVMOptions") {
        log.info("getDaemonJVMOptions: $daemonJVMOptions")// + daemonJVMOptions.mappers.flatMap { it.toArgs("-") })
        CompileService.CallResult.Good(daemonJVMOptions)
    }

    override suspend fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> {
        log.info("fun registerClient")
        return ifAlive(minAliveness = Aliveness.Alive, info = "registerClient") {
            registerClientImpl(aliveFlagPath)
        }
    }

    override suspend fun classesFqNamesByFiles(
        sessionId: Int, sourceFiles: Set<File>
    ): CompileService.CallResult<Set<String>> =
        ifAlive {
            withValidClientOrSessionProxy(sessionId) {
                CompileService.CallResult.Good(classesFqNames(sourceFiles))
            }
        }

    private fun registerClientImpl(aliveFlagPath: String?): CompileService.CallResult<Nothing> {
        state.addClient(aliveFlagPath)
        log.info("Registered a client alive file: $aliveFlagPath")
        return CompileService.CallResult.Ok()
    }

    override suspend fun getClients(): CompileService.CallResult<List<String>> = ifAlive(info = "getClients") {
        getClientsImpl()
    }

    private fun getClientsImpl() = CompileService.CallResult.Good(state.getClientsFlagPaths())

    // TODO: consider tying a session to a client and use this info to cleanup
    override suspend fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> =
        ifAlive(minAliveness = Aliveness.Alive, info = "leaseCompileSession") {
            CompileService.CallResult.Good(
                state.sessions.leaseSession(ClientOrSessionProxy<Any>(aliveFlagPath)).apply {
                    log.info("leased a new session $this, session alive file: $aliveFlagPath")
                })
        }

    override suspend fun releaseCompileSession(sessionId: Int) = ifAlive(
        minAliveness = Aliveness.LastSession,
        info = "releaseCompileSession"
    ) {
        state.sessions.remove(sessionId)
        log.info("cleaning after session $sessionId")
        val completed = CompletableDeferred<Boolean>()
        queriesActor.send(ExclusiveTask(completed, { clearJarCache() }))
//        completed.await()
        if (state.sessions.isEmpty()) {
            // TODO: and some goes here
        }
        timer.schedule(0) {
            periodicAndAfterSessionCheck()
        }
        CompileService.CallResult.Ok()
    }


    override suspend fun checkCompilerId(expectedCompilerId: CompilerId): Boolean =
        (compilerId.compilerVersion.isEmpty() || compilerId.compilerVersion == expectedCompilerId.compilerVersion) &&
                (compilerId.compilerClasspath.all { expectedCompilerId.compilerClasspath.contains(it) }) &&
                !classpathWatcher.isChanged

    override suspend fun getUsedMemory(): CompileService.CallResult<Long> =
        ifAlive(info = "getUsedMemory") { CompileService.CallResult.Good(usedMemory(withGC = true)) }

    override suspend fun shutdown(): CompileService.CallResult<Nothing> =
        ifAliveExclusive(minAliveness = Aliveness.LastSession, info = "shutdown") {
            shutdownWithDelay()
            CompileService.CallResult.Ok()
        }

    override suspend fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> =
        ifAlive(minAliveness = Aliveness.LastSession, info = "scheduleShutdown") {
            scheduleShutdownImpl(graceful)
        }

    private fun scheduleShutdownImpl(graceful: Boolean): CompileService.CallResult<Boolean> {
        val res = when {
            graceful -> gracefulShutdown(true)
            else -> {
                shutdownWithDelay()
                true
            }
        }
        return CompileService.CallResult.Good(res)
    }

    override suspend fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        compilationResults: CompilationResultsAsync?
    ): CompileService.CallResult<Int> = ifAlive(info = "compile") {
        log.info("servicesFacade : $servicesFacade")
        servicesFacade.report(ReportCategory.DAEMON_MESSAGE, ReportSeverity.INFO, "abacaba")
        log.info("servicesFacade - sent \"abacaba\"")
        val messageCollector = CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
        val daemonReporter = DaemonMessageReporterAsync(servicesFacade, compilationOptions)
        val targetPlatform = compilationOptions.targetPlatform
        log.info("Starting compilation with args: " + compilerArguments.joinToString(" "))

        @Suppress("UNCHECKED_CAST")
        val compiler = when (targetPlatform) {
            CompileService.TargetPlatform.JVM -> K2JVMCompiler()
            CompileService.TargetPlatform.JS -> K2JSCompiler()
            CompileService.TargetPlatform.METADATA -> K2MetadataCompiler()
        } as CLICompiler<CommonCompilerArguments>

        val k2PlatformArgs = compiler.createArguments()
        parseCommandLineArguments(compilerArguments.asList(), k2PlatformArgs)
        val argumentParseError = validateArguments(k2PlatformArgs.errors)
        if (argumentParseError != null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, argumentParseError)
            CompileService.CallResult.Good(ExitCode.COMPILATION_ERROR.code)
        } else when (compilationOptions.compilerMode) {
            CompilerMode.JPS_COMPILER -> {
                val jpsServicesFacade = servicesFacade as CompilerCallbackServicesFacadeClientSide

                withIC(enabled = servicesFacade.hasIncrementalCaches()) {
                    doCompile(sessionId, daemonReporter, tracer = null) { eventManger, profiler ->
                        val services = createCompileServices(jpsServicesFacade, eventManger, profiler).await()
                        compiler.exec(messageCollector, services, k2PlatformArgs)
                    }.await()
                }
            }
            CompilerMode.NON_INCREMENTAL_COMPILER -> {
                doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                    log.info("(in doCompile's body) - start")
                    compiler.exec(messageCollector, Services.EMPTY, k2PlatformArgs).also {
                        log.info("(in doCompile's body) - end")
                    }
                }.await()
            }
            CompilerMode.INCREMENTAL_COMPILER -> {
                val gradleIncrementalArgs = compilationOptions as IncrementalCompilationOptions
                val gradleIncrementalServicesFacade = servicesFacade as IncrementalCompilerServicesFacadeAsync

                when (targetPlatform) {
                    CompileService.TargetPlatform.JVM -> {
                        val k2jvmArgs = k2PlatformArgs as K2JVMCompilerArguments
                        withIC {
                            doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                                execIncrementalCompiler(
                                    k2jvmArgs, gradleIncrementalArgs, gradleIncrementalServicesFacade, compilationResults,
                                    messageCollector, daemonReporter
                                )
                            }.await()
                        }
                    }
                    CompileService.TargetPlatform.JS -> {
                        val k2jsArgs = k2PlatformArgs as K2JSCompilerArguments

                        withJsIC {
                            doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                                execJsIncrementalCompiler(
                                    k2jsArgs,
                                    gradleIncrementalArgs,
                                    gradleIncrementalServicesFacade,
                                    compilationResults,
                                    messageCollector
                                )
                            }.await()
                        }
                    }
                    else -> throw IllegalStateException("Incremental compilation is not supported for target platform: $targetPlatform")

                }
            }
            else -> throw IllegalStateException("Unknown compilation mode ${compilationOptions.compilerMode}")
        }
    }

    private fun execJsIncrementalCompiler(
        args: K2JSCompilerArguments,
        incrementalCompilationOptions: IncrementalCompilationOptions,
        servicesFacade: IncrementalCompilerServicesFacadeAsync,
        compilationResults: CompilationResultsAsync?,
        compilerMessageCollector: MessageCollector
    ): ExitCode {
        val allKotlinFiles = arrayListOf<File>()
        val freeArgsWithoutKotlinFiles = arrayListOf<String>()
        args.freeArgs.forEach {
            if (it.endsWith(".kt") && File(it).exists()) {
                allKotlinFiles.add(File(it))
            } else {
                freeArgsWithoutKotlinFiles.add(it)
            }
        }
        args.freeArgs = freeArgsWithoutKotlinFiles

        val reporter = RemoteICReporterAsync(servicesFacade, compilationResults, incrementalCompilationOptions)

        val changedFiles = if (incrementalCompilationOptions.areFileChangesKnown) {
            ChangedFiles.Known(incrementalCompilationOptions.modifiedFiles.orEmpty(), incrementalCompilationOptions.deletedFiles.orEmpty())
        } else {
            ChangedFiles.Unknown()
        }

        val workingDir = incrementalCompilationOptions.workingDir
        val modulesApiHistory = ModulesApiHistoryJs(incrementalCompilationOptions.modulesInfo)

        val compiler = IncrementalJsCompilerRunner(
            workingDir = workingDir,
            reporter = reporter,
            buildHistoryFile = incrementalCompilationOptions.multiModuleICSettings.buildHistoryFile,
            modulesApiHistory = modulesApiHistory
        )
        return compiler.compile(allKotlinFiles, args, compilerMessageCollector, changedFiles)
    }

    private fun execIncrementalCompiler(
        k2jvmArgs: K2JVMCompilerArguments,
        incrementalCompilationOptions: IncrementalCompilationOptions,
        servicesFacade: IncrementalCompilerServicesFacadeAsync,
        compilationResults: CompilationResultsAsync?,
        compilerMessageCollector: MessageCollector,
        daemonMessageReporterAsync: DaemonMessageReporterAsync
    ): ExitCode {
        val reporter = RemoteICReporterAsync(servicesFacade, compilationResults, incrementalCompilationOptions)

        val moduleFile = k2jvmArgs.buildFile?.let(::File)
        assert(moduleFile?.exists() ?: false) { "Module does not exist ${k2jvmArgs.buildFile}" }

        // todo: pass javaSourceRoots and allKotlinFiles using IncrementalCompilationOptions
        val parsedModule = run {
            val bytesOut = ByteArrayOutputStream()
            val printStream = PrintStream(bytesOut)
            val mc = PrintingMessageCollector(printStream, MessageRenderer.PLAIN_FULL_PATHS, false)
            val parsedModule = ModuleXmlParser.parseModuleScript(k2jvmArgs.buildFile!!, mc)
            if (mc.hasErrors()) {
                daemonMessageReporterAsync.report(ReportSeverity.ERROR, bytesOut.toString("UTF8"))
            }
            parsedModule
        }

        val javaSourceRoots = parsedModule.modules.flatMapTo(HashSet()) {
            it.getJavaSourceRoots().map { JvmSourceRoot(File(it.path), it.packagePrefix) }
        }

        k2jvmArgs.commonSources = parsedModule.modules.flatMap { it.getCommonSourceFiles() }.toTypedArray().takeUnless { it.isEmpty() }

        val allKotlinFiles = parsedModule.modules.flatMap { it.getSourceFiles().map(::File) }
        val allKotlinExtensions = (
                DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS +
                        allKotlinFiles.asSequence()
                            .map { it.extension }
                            .filter { !it.equals("java", ignoreCase = true) }
                            .asIterable()
                ).distinct()
        k2jvmArgs.friendPaths = parsedModule.modules.flatMap(Module::getFriendPaths).toTypedArray()

        val changedFiles = if (incrementalCompilationOptions.areFileChangesKnown) {
            ChangedFiles.Known(incrementalCompilationOptions.modifiedFiles!!, incrementalCompilationOptions.deletedFiles!!)
        } else {
            ChangedFiles.Unknown()
        }

        val workingDir = incrementalCompilationOptions.workingDir

        val modulesApiHistory = incrementalCompilationOptions.run {
            if (!multiModuleICSettings.useModuleDetection) {
                ModulesApiHistoryJvm(modulesInfo)
            } else {
                ModulesApiHistoryAndroid(modulesInfo)
            }
        }

        val compiler = IncrementalJvmCompilerRunner(
            workingDir,
            javaSourceRoots,
            reporter,
            buildHistoryFile = incrementalCompilationOptions.multiModuleICSettings.buildHistoryFile,
            localStateDirs = incrementalCompilationOptions.localStateDirs,
            usePreciseJavaTracking = incrementalCompilationOptions.usePreciseJavaTracking,
            modulesApiHistory = modulesApiHistory,
            kotlinSourceFilesExtensions = allKotlinExtensions
        )
        return compiler.compile(allKotlinFiles, k2jvmArgs, compilerMessageCollector, changedFiles)
    }

    override suspend fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        templateClasspath: List<File>,
        templateClassName: String
    ): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive, info = "leaseReplSession") {
        if (compilationOptions.targetPlatform != CompileService.TargetPlatform.JVM)
            CompileService.CallResult.Error("Sorry, only JVM target platform is supported now")
        else {
            val disposable = Disposer.newDisposable()
            val messageCollector =
                CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
            val repl = KotlinJvmReplServiceAsync(
                disposable, serverSocketWithPort, templateClasspath, templateClassName,
                messageCollector
            )
            val sessionId = state.sessions.leaseSession(ClientOrSessionProxy(aliveFlagPath, repl, disposable))

            CompileService.CallResult.Good(sessionId)
        }
    }

    // TODO: add more checks (e.g. is it a repl session)
    override suspend fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> = releaseCompileSession(sessionId)

    override suspend fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacadeClientSide> =
        ifAlive(minAliveness = Aliveness.Alive, info = "replCreateState") {
            withValidRepl(sessionId) {
                CompileService.CallResult.Good(
                    createRemoteState(findReplServerSocket()).clientSide
                )
            }
        }

    override suspend fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCheckResult> = ifAlive(minAliveness = Aliveness.Alive, info = "replCheck") {
        withValidRepl(sessionId) {
            withValidReplState(replStateId) { state ->
                check(state, codeLine)
            }
        }
    }

    override suspend fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCompileResult> =
        ifAlive(minAliveness = Aliveness.Alive, info = "replCompile") {
            withValidRepl(sessionId) {
                withValidReplState(replStateId) { state ->
                    compile(state, codeLine)
                }
            }
        }

    // -----------------------------------------------------------------------
    // internal implementation stuff

    // TODO: consider matching compilerId coming from outside with actual one
    //    private val selfCompilerId by lazy {
    //        CompilerId(
    //                compilerClasspath = System.getProperty("java.class.path")
    //                                            ?.split(File.pathSeparator)
    //                                            ?.map { File(it) }
    //                                            ?.filter { it.exists() }
    //                                            ?.map { it.absolutePath }
    //                                    ?: listOf(),
    //                compilerVersion = loadKotlinVersionFromResource()
    //        )
    //    }

    init {

        log.info("init(port= $serverSocketWithPort)")

        // assuming logically synchronized
        System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        // TODO UNCOMMENT THIS : this.toRMIServer(daemonOptions, compilerId) // also create RMI server in order to support old clients
        this.toRMIServer(daemonOptions, compilerId)

        KeepAliveServer.runServer()

        timer.schedule(10) {
            exceptionLoggingTimerThread(info = "initiateElections") {
                println("-initiateElections-")
                initiateElections()
            }
        }
        timer.schedule(delay = DAEMON_PERIODIC_CHECK_INTERVAL_MS, period = DAEMON_PERIODIC_CHECK_INTERVAL_MS) {
            exceptionLoggingTimerThread(info = "periodicAndAfterSessionCheck") { periodicAndAfterSessionCheck() }
        }
        timer.schedule(delay = DAEMON_PERIODIC_SELDOM_CHECK_INTERVAL_MS + 100, period = DAEMON_PERIODIC_SELDOM_CHECK_INTERVAL_MS) {
            exceptionLoggingTimerThread(info = "periodicSeldomCheck") { periodicSeldomCheck() }
        }
        log.info("last_init_end")
    }

    private fun exceptionLoggingTimerThread(info: String = "no info", body: () -> Unit) {
        try {
            println("exceptionLoggingTimerThread body($info) : starting...")
            body()
            println("exceptionLoggingTimerThread body($info) : finishec(OK)")
        } catch (e: Throwable) {
            System.err.println("[$info] Exception in timer thread: " + e.message)
            e.printStackTrace(System.err)
            log.log(Level.SEVERE, "[$info] Exception in timer thread", e)
        }
    }

    private fun periodicAndAfterSessionCheck() {

        if (state.delayedShutdownQueued.get()) return

        val anyDead = state.sessions.cleanDead() || state.cleanDeadClients()

        async {
            ifAliveUnit(minAliveness = Aliveness.LastSession, info = "periodicAndAfterSessionCheck - 1") {
                when {
                // check if in graceful shutdown state and all sessions are closed
                    state.alive.get() == Aliveness.LastSession.ordinal && state.sessions.isEmpty() -> {
                        log.info("All sessions finished")
                        shutdownWithDelay()
                        return@ifAliveUnit
                    }
                    state.aliveClientsCount == 0 -> {
                        log.info("No more clients left")
                        shutdownWithDelay()
                        return@ifAliveUnit
                    }
                // discovery file removed - shutdown
                    !runFile.exists() -> {
                        log.info("Run file removed")
                        shutdownWithDelay()
                        return@ifAliveUnit
                    }
                }
            }


            ifAliveUnit(minAliveness = Aliveness.Alive, info = "periodicAndAfterSessionCheck - 2") {
                when {
                    daemonOptions.autoshutdownUnusedSeconds != COMPILE_DAEMON_TIMEOUT_INFINITE_S && compilationsCounter.get() == 0 && nowSeconds() - lastUsedSeconds > daemonOptions.autoshutdownUnusedSeconds -> {
                        log.info("Unused timeout exceeded ${daemonOptions.autoshutdownUnusedSeconds}s")
                        gracefulShutdown(false)
                    }
                    daemonOptions.autoshutdownIdleSeconds != COMPILE_DAEMON_TIMEOUT_INFINITE_S && nowSeconds() - lastUsedSeconds > daemonOptions.autoshutdownIdleSeconds -> {
                        log.info("Idle timeout exceeded ${daemonOptions.autoshutdownIdleSeconds}s")
                        gracefulShutdown(false)
                    }
                    anyDead -> {
                        clearJarCache()
                    }
                }
            }
        }
    }

    private fun periodicSeldomCheck() {
        async {
            ifAliveUnit(minAliveness = Aliveness.Alive, info = "periodicSeldomCheck") {
                // compiler changed (seldom check) - shutdown
                if (classpathWatcher.isChanged) {
                    log.info("Compiler changed.")
                    gracefulShutdown(false)
                }
            }
        }
    }


    // TODO: handover should include mechanism for client to switch to a new daemon then previous "handed over responsibilities" and shot down
    private fun initiateElections() {
        runBlocking(Unconfined) {
            ifAliveUnit(info = "initiateElections") {
                log.info("initiate elections")
                val aliveWithOpts = walkDaemonsAsync(
                    File(daemonOptions.runFilesPathOrDefault),
                    compilerId,
                    runFile,
                    filter = { _, p -> p != port },
                    report = { _, msg -> log.info(msg) },
                    useRMI = false
                )
                log.info("aliveWithOpts : ${aliveWithOpts.map { it.daemon.javaClass.name }}")
                val comparator = compareByDescending<DaemonWithMetadataAsync, DaemonJVMOptions>(
                    DaemonJVMOptionsMemoryComparator(),
                    { it.jvmOptions }
                )
                    .thenBy {
                        when (it.daemon) {
                            is CompileServiceAsyncWrapper -> 0
                            else -> 1
                        }
                    }
                    .thenBy(FileAgeComparator()) { it.runFile }
                    .thenBy { it.daemon.serverPort }
                aliveWithOpts.maxWith(comparator)?.let { bestDaemonWithMetadata ->
                    val fattestOpts = bestDaemonWithMetadata.jvmOptions
                    if (fattestOpts memorywiseFitsInto daemonJVMOptions && FileAgeComparator().compare(
                            bestDaemonWithMetadata.runFile,
                            runFile
                        ) < 0
                    ) {
                        // all others are smaller that me, take overs' clients and shut them down
                        log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE lower prio, taking clients from them and schedule them to shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                        aliveWithOpts.forEach { (daemon, runFile, _) ->
                            try {
                                log.info("other : $daemon")
                                daemon.getClients().takeIf { it.isGood }?.let {
                                    it.get().forEach { clientAliveFile ->
                                        registerClientImpl(clientAliveFile)
                                    }
                                }
                                log.info("other : CLIENTS_OK")
                                daemon.scheduleShutdown(true)
                                log.info("other : SHUTDOWN_OK")
                            } catch (e: Throwable) {
                                log.info("Cannot connect to a daemon, assuming dying ('${runFile.canonicalPath}'): ${e.message}")
                            }
                        }
                    }
                    // TODO: seems that the second part of condition is incorrect, reconsider:
                    // the comment by @tsvtkv from review:
                    //    Algorithm in plain english:
                    //    (1) If the best daemon fits into me and the best daemon is younger than me, then I take over all other daemons clients.
                    //    (2) If I fit into the best daemon and the best daemon is older than me, then I give my clients to that daemon.
                    //
                    //    For example:
                    //
                    //    daemon A starts with params: maxMem=100, codeCache=50
                    //    daemon B starts with params: maxMem=200, codeCache=50
                    //    daemon C starts with params: maxMem=150, codeCache=100
                    //    A performs election: (1) is false because neither B nor C does not fit into A, (2) is false because both B and C are younger than A.
                    //    B performs election: (1) is false because neither A nor C does not fit into B, (2) is false because B does not fit into neither A nor C.
                    //    C performs election: (1) is false because B is better than A and B does not fit into C, (2) is false C does not fit into neither A nor B.
                    //    Result: all daemons are alive and well.
                    else if (daemonJVMOptions memorywiseFitsInto fattestOpts && FileAgeComparator().compare(
                            bestDaemonWithMetadata.runFile,
                            runFile
                        ) > 0
                    ) {
                        // there is at least one bigger, handover my clients to it and shutdown
                        log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE higher prio, handover clients to it and schedule shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                        getClientsImpl().takeIf { it.isGood }?.let {
                            it.get().forEach { bestDaemonWithMetadata.daemon.registerClient(it) }
                        }
                        scheduleShutdownImpl(true)
                    } else {
                        // undecided, do nothing
                        log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE equal prio, continue: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                        // TODO: implement some behaviour here, e.g.:
                        //   - shutdown/takeover smaller daemon
                        //   - runServer (or better persuade client to runServer) a bigger daemon (in fact may be even simple shutdown will do, because of client's daemon choosing logic)
                    }
                }

            }
        }
    }

    private fun shutdownNow() {
        log.info("Shutdown started")
        fun Long.mb() = this / (1024 * 1024)
        with(Runtime.getRuntime()) {
            log.info("Memory stats: total: ${totalMemory().mb()}mb, free: ${freeMemory().mb()}mb, max: ${maxMemory().mb()}mb")
        }
        state.alive.set(Aliveness.Dying.ordinal)
        downServer()
        log.info("Shutdown complete")
        onShutdown()
        log.handlers.forEach { it.flush() }
    }

    private fun shutdownWithDelayImpl(currentClientsCount: Int, currentSessionId: Int, currentCompilationsCount: Int) {
        log.info("${log.name} .......shutdowning........")
        log.info("${log.name} currentCompilationsCount = $currentCompilationsCount, compilationsCounter.get(): ${compilationsCounter.get()}")
        state.delayedShutdownQueued.set(false)
        if (currentClientsCount == state.clientsCounter &&
            currentCompilationsCount == compilationsCounter.get() &&
            currentSessionId == state.sessions.lastSessionId
        ) {
            log.info("currentCompilationsCount == compilationsCounter.get()")
            runBlocking(Unconfined) {
                ifAliveExclusiveUnit(minAliveness = Aliveness.LastSession, info = "initiate elections - shutdown") {
                    log.info("Execute delayed shutdown!!!")
                    log.fine("Execute delayed shutdown")
                    shutdownNow()
                }
            }
        } else {
            log.info("Cancel delayed shutdown due to a new activity")
        }
    }

    private fun shutdownWithDelay() {
        state.delayedShutdownQueued.set(true)
        val currentClientsCount = state.clientsCounter
        val currentSessionId = state.sessions.lastSessionId
        val currentCompilationsCount = compilationsCounter.get()
        log.info("Delayed shutdown in ${daemonOptions.shutdownDelayMilliseconds}ms")
        timer.schedule(daemonOptions.shutdownDelayMilliseconds) {
            shutdownWithDelayImpl(currentClientsCount, currentSessionId, currentCompilationsCount)
        }
    }

    private fun gracefulShutdown(onAnotherThread: Boolean): Boolean {

        if (!state.alive.compareAndSet(Aliveness.Alive.ordinal, Aliveness.LastSession.ordinal)) {
            log.info("Invalid state for graceful shutdown: ${state.alive.get().toAlivenessName()}")
            return false
        }
        log.info("Graceful shutdown signalled")

        if (!onAnotherThread) {
            shutdownIfIdle()
        } else {
            timer.schedule(1) {
                gracefulShutdownImpl()
            }

        }
        return true
    }

    private fun gracefulShutdownImpl() {
        runBlocking(Unconfined) {
            ifAliveExclusiveUnit(minAliveness = Aliveness.LastSession, info = "gracefulShutdown") {
                shutdownIfIdle()
            }
        }
    }

    private fun shutdownIfIdle() = when {
        state.sessions.isEmpty() -> shutdownWithDelay()
        else -> {
            daemonOptions.autoshutdownIdleSeconds =
                    TimeUnit.MILLISECONDS.toSeconds(daemonOptions.forceShutdownTimeoutMilliseconds).toInt()
            daemonOptions.autoshutdownUnusedSeconds = daemonOptions.autoshutdownIdleSeconds
            log.info("Some sessions are active, waiting for them to finish")
            log.info("Unused/idle timeouts are set to ${daemonOptions.autoshutdownUnusedSeconds}/${daemonOptions.autoshutdownIdleSeconds}s")
        }
    }

    private fun doCompile(
        sessionId: Int,
        daemonMessageReporterAsync: DaemonMessageReporterAsync,
        tracer: RemoteOperationsTracer?,
        body: suspend (EventManager, Profiler) -> ExitCode
    ): Deferred<CompileService.CallResult<Int>> = async {
        log.info("alive!")
        withValidClientOrSessionProxy(sessionId) {
            log.info("before compile")
            val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
            val eventManger = EventManagerImpl()
            try {
                log.info("trying get exitCode")
                val exitCode = checkedCompile(daemonMessageReporterAsync, rpcProfiler) {
                    log.info("body of exitCode")
                    body(eventManger, rpcProfiler).code.also {
                        log.info("after body of exitCode")
                    }
                }.await()
                log.info("got exitCode")
                CompileService.CallResult.Good(exitCode)
            } finally {
                eventManger.fireCompilationFinished()
                log.info("after compile")
            }
        }
    }

    private fun createCompileServices(
        facade: CompilerCallbackServicesFacadeClientSide,
        eventManager: EventManager,
        rpcProfiler: Profiler
    ): Deferred<Services> = async {
        val builder = Services.Builder()
        if (facade.hasIncrementalCaches()) {
            builder.register(
                IncrementalCompilationComponents::class.java,
                RemoteIncrementalCompilationComponentsClient(facade, eventManager, rpcProfiler)
            )
        }
        if (facade.hasLookupTracker()) {
            builder.register(LookupTracker::class.java, RemoteLookupTrackerClient(facade, eventManager, rpcProfiler))
        }
        if (facade.hasCompilationCanceledStatus()) {
            log.info("facade.hasCompilationCanceledStatus() = true")
            builder.register(CompilationCanceledStatus::class.java, RemoteCompilationCanceledStatusClient(facade, rpcProfiler))
        } else {
            log.info("facade.hasCompilationCanceledStatus() = false")
        }
        builder.build()
    }


    private fun <R> checkedCompile(
        daemonMessageReporterAsync: DaemonMessageReporterAsync,
        rpcProfiler: Profiler,
        body: suspend () -> R
    ): Deferred<R> = async {
        try {
            log.info("checkedCompile")
            val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

            val res = profiler.withMeasure(null, body)

            val endMem = if (daemonOptions.reportPerf) usedMemory(withGC = false) else 0L

            log.info("Done with result " + res.toString())

            if (daemonOptions.reportPerf) {
                fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
                fun Long.kb() = this / 1024
                val pc = profiler.getTotalCounters()
                val rpc = rpcProfiler.getTotalCounters()

                "PERF: Compile on daemon: ${pc.time.ms()} ms; thread: user ${pc.threadUserTime.ms()} ms, sys ${(pc.threadTime - pc.threadUserTime).ms()} ms; rpc: ${rpc.count} calls, ${rpc.time.ms()} ms, thread ${rpc.threadTime.ms()} ms; memory: ${endMem.kb()} kb (${"%+d".format(
                    pc.memory.kb()
                )} kb)".let {
                    daemonMessageReporterAsync.report(ReportSeverity.INFO, it)
                    log.info(it)
                }

                // this will only be reported if if appropriate (e.g. ByClass) profiler is used
                for ((obj, counters) in rpcProfiler.getCounters()) {
                    "PERF: rpc by $obj: ${counters.count} calls, ${counters.time.ms()} ms, thread ${counters.threadTime.ms()} ms".let {
                        daemonMessageReporterAsync.report(ReportSeverity.INFO, it)
                        log.info(it)
                    }
                }
            }
            res
        }
        // TODO: consider possibilities to handle OutOfMemory
        catch (e: Throwable) {
            log.info("Error: $e")
            throw e
        }
    }

    override suspend fun clearJarCache() {
        ZipHandler.clearFileAccessorCache()
        (KotlinCoreEnvironment.applicationEnvironment?.jarFileSystem as? CoreJarFileSystem)?.clearHandlersCache()
    }

    private suspend fun <R> ifAlive(
        minAliveness: Aliveness = Aliveness.LastSession,
        info: String = "no info",
        body: suspend () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        log.info("ifAlive(1)($info)")
        val result = CompletableDeferred<Any>()
        queriesActor.send(OrdinaryTaskWithResult(result) {
            log.info("ifAlive(2)($info)")
            ifAliveChecksImpl(minAliveness, info, body).also {
                log.info("ifAlive(3)($info)")
            }
        })
        return result.await() as CompileService.CallResult<R>
    }

    private suspend fun ifAliveUnit(
        minAliveness: Aliveness = Aliveness.LastSession,
        info: String = "no info",
        body: suspend () -> Unit
    ) {
        log.info("ifAliveUnit(1)($info)")
        val completed = CompletableDeferred<Boolean>()
        queriesActor.send(
            OrdinaryTask(completed) {
                log.info("ifAliveUnit(2)($info)")
                ifAliveChecksImpl(minAliveness, info) {
                    body()
                    CompileService.CallResult.Ok()
                }.also {
                    log.info("ifAliveUnit(3)($info)")
                }
            }
        )
        completed.await()
    }

    private suspend fun <R> ifAliveExclusive(
        minAliveness: Aliveness = Aliveness.LastSession,
        info: String = "no info",
        body: suspend () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        log.info("ifAliveExclusive(1)($info)")
        val result = CompletableDeferred<Any>()
        queriesActor.send(ShutdownTaskWithResult(result) {
            log.info("ifAliveExclusive(2)($info)")
            ifAliveChecksImpl(minAliveness, info, body).also {
                log.info("ifAliveExclusive(3)($info)")
            }
        })
        return result.await() as CompileService.CallResult<R>
    }

    private suspend fun ifAliveExclusiveUnit(
        minAliveness: Aliveness = Aliveness.LastSession,
        info: String = "no info",
        body: suspend () -> Unit
    ): CompileService.CallResult<Unit> {
        log.info("ifAliveExclusiveUnit(1)($info)")
        val result = CompletableDeferred<Any>()
        queriesActor.send(ShutdownTaskWithResult(result) {
            log.info("ifAliveExclusive(2)($info)")
            ifAliveChecksImpl(minAliveness) {
                body()
                CompileService.CallResult.Ok()
            }.also {
                log.info("ifAliveExclusive(3)($info)")
            }
        })
        return result.await() as CompileService.CallResult<Unit>
    }

    private suspend fun <R> ifAliveChecksImpl(
        minAliveness: Aliveness = Aliveness.LastSession,
        info: String = "no info",
        body: suspend () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        val curState = state.alive.get()
        log.info("ifAliveChecksImpl.info = $info")
        return when {
            curState < minAliveness.ordinal -> {
                log.info("Cannot perform operation, requested state: ${minAliveness.name} > actual: ${curState.toAlivenessName()}")
                CompileService.CallResult.Dying()
            }
            else -> {
                try {
                    body()
                } catch (e: Throwable) {
                    log.log(Level.SEVERE, "Exception", e)
                    CompileService.CallResult.Error(e.message ?: "unknown")
                }
            }
        }
    }

    private inline fun <R> withValidClientOrSessionProxy(
        sessionId: Int,
        body: (ClientOrSessionProxy<Any>?) -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        val session: ClientOrSessionProxy<Any>? =
            if (sessionId == CompileService.NO_SESSION) null
            else state.sessions[sessionId] ?: return CompileService.CallResult.Error("Unknown or invalid session $sessionId")
        try {
            compilationsCounter.incrementAndGet()
            return body(session)
        } finally {
            _lastUsedSeconds = nowSeconds()
        }
    }

    private inline fun <R> withValidRepl(sessionId: Int, body: KotlinJvmReplServiceAsync.() -> R): CompileService.CallResult<R> =
        withValidClientOrSessionProxy(sessionId) { session ->
            (session?.data as? KotlinJvmReplServiceAsync?)?.let {
                CompileService.CallResult.Good(it.body())
            } ?: CompileService.CallResult.Error("Not a REPL session $sessionId")
        }

    @JvmName("withValidRepl1")
    private inline fun <R> withValidRepl(
        sessionId: Int,
        body: KotlinJvmReplServiceAsync.() -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> =
        withValidClientOrSessionProxy(sessionId) { session ->
            (session?.data as? KotlinJvmReplServiceAsync?)?.body() ?: CompileService.CallResult.Error("Not a REPL session $sessionId")
        }

}


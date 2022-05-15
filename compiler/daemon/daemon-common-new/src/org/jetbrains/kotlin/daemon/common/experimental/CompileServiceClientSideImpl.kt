/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.impls.CompilerServicesFacadeBase
import java.io.File
import java.util.logging.Logger

class CompileServiceClientSideImpl(
    override val serverPort: Int,
    val serverHost: String,
    val serverFile: File
) : CompileServiceClientSide,
    Client<CompileServiceServerSide> by object : DefaultAuthorizableClient<CompileServiceServerSide>(
        serverPort,
        serverHost
    ) {

        private fun nowMillieconds() = System.currentTimeMillis()

        @Volatile
        private var lastUsedMilliSeconds: Long = nowMillieconds()

        private fun deltaTime() = nowMillieconds() - lastUsedMilliSeconds

        private fun keepAliveSuccess() = deltaTime() < KEEPALIVE_PERIOD

        override suspend fun authorizeOnServer(serverOutputChannel: ByteWriteChannelWrapper): Boolean =
            runWithTimeout {
                log.info("in authoriseOnServer(serverFile=$serverFile)")
                val signature = serverFile.inputStream().use(::readTokenKeyPairAndSign)
                sendSignature(serverOutputChannel, signature)
                true
            } ?: false

        override suspend fun clientHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger): Boolean {
            return trySendHandshakeMessage(output, log) && tryAcquireHandshakeMessage(input, log)
        }

        override suspend fun startKeepAlives() {
            val keepAliveMessage = Server.KeepAliveMessage<CompileServiceServerSide>()
            async(newSingleThreadContext("keepAliveThread")) {
                delay(KEEPALIVE_PERIOD * 4)
                while (true) {
                    delay(KEEPALIVE_PERIOD)
//                    println("[$this] KEEPALIVE_PERIOD")
                    while (keepAliveSuccess()) {
//                        println("[$this] remained ${KEEPALIVE_PERIOD - deltaTime()}")
                        delay(KEEPALIVE_PERIOD - deltaTime())
                    }
                    runWithTimeout(timeout = KEEPALIVE_PERIOD / 2) {
                        //                        println("[$this] sent keepalive")
                        val id = sendMessage(keepAliveMessage)
                        readMessage<Server.KeepAliveAcknowledgement<*>>(id)
                    } ?: if (!keepAliveSuccess()) readActor.send(StopAllRequests()).also {
                        //                        println("[$this] got keepalive")
                    }
                }
            }
        }

        override suspend fun delayKeepAlives() {
//            println("[$this] delayKeepAlives")
            lastUsedMilliSeconds = nowMillieconds()
        }

    } {
    override suspend fun classesFqNamesByFiles(sessionId: Int, sourceFiles: Set<File>): CompileService.CallResult<Set<String>> {
        val id = sendMessage(ClassesFqNamesByFilesMessage(sessionId, sourceFiles))
        return readMessage(id)
    }

    val log = Logger.getLogger("CompileServiceClientSideImpl")

    override suspend fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        compilationResults: CompilationResultsAsync?
    ): CompileService.CallResult<Int> {
        log.info("override fun compile(")
        val id = sendMessage(CompileMessage(
            sessionId,
            compilerArguments,
            compilationOptions,
            servicesFacade,
            compilationResults
        ))
        log.info("override fun compile(: id = $id")
        return readMessage(id)
    }

    override suspend fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        templateClasspath: List<File>,
        templateClassName: String
    ): CompileService.CallResult<Int> {
        val id = sendMessage(
            LeaseReplSessionMessage(
                aliveFlagPath,
                compilerArguments,
                compilationOptions,
                servicesFacade,
                templateClasspath,
                templateClassName
            )
        )
        return readMessage(id)
    }

    // CompileService methods:

    override suspend fun checkCompilerId(expectedCompilerId: CompilerId): Boolean {
        val id = sendMessage(
            CheckCompilerIdMessage(
                expectedCompilerId
            )
        )
        return readMessage(id)
    }

    override suspend fun getUsedMemory(): CompileService.CallResult<Long> {
        val id = sendMessage(GetUsedMemoryMessage())
        return readMessage(id)
    }


    override suspend fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> {
        val id = sendMessage(GetDaemonOptionsMessage())
        return readMessage(id)
    }

    override suspend fun getDaemonInfo(): CompileService.CallResult<String> {
        val id = sendMessage(GetDaemonInfoMessage())
        return readMessage(id)
    }

    override suspend fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> {
        log.info("sending message (GetDaemonJVMOptionsMessage) ... (deaemon port = $serverPort)")
        val id = sendMessage(GetDaemonJVMOptionsMessage())
        log.info("message is sent!")
        log.info("reading message...")
        val res = readMessage<CompileService.CallResult<DaemonJVMOptions>>(id)
        log.info("reply : $res")
        return res
    }

    override suspend fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> {
        log.info("registerClient")
//        println("client's fun registerClient")
        val id = sendMessage(RegisterClientMessage(aliveFlagPath))
        return readMessage(id)
    }

    override suspend fun getClients(): CompileService.CallResult<List<String>> {
        val id = sendMessage(GetClientsMessage())
        return readMessage(id)
    }

    override suspend fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> {
        val id = sendMessage(
            LeaseCompileSessionMessage(
                aliveFlagPath
            )
        )
        return readMessage(id)
    }

    override suspend fun releaseCompileSession(sessionId: Int): CompileService.CallResult<Nothing> {
        val id = sendMessage(
            ReleaseCompileSessionMessage(
                sessionId
            )
        )
        return readMessage(id)
    }

    override suspend fun shutdown(): CompileService.CallResult<Nothing> {
        val id = sendMessage(ShutdownMessage())
        log.info("ShutdownMessage_id = $id")
        val res = readMessage<CompileService.CallResult<Nothing>>(id)
        log.info("ShutdownMessage_res : $res")
        return res
    }

    override suspend fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> {
        val id = sendMessage(ScheduleShutdownMessage(graceful))
        return readMessage(id)
    }

    override suspend fun clearJarCache() {
        val id = sendMessage(ClearJarCacheMessage())
    }

    override suspend fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> {
        val id = sendMessage(ReleaseReplSessionMessage(sessionId))
        return readMessage(id)
    }

    override suspend fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacadeAsync> {
        val id = sendMessage(ReplCreateStateMessage(sessionId))
        return readMessage(id)
    }

    override suspend fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCheckResult> {
        val id = sendMessage(
            ReplCheckMessage(
                sessionId,
                replStateId,
                codeLine
            )
        )
        return readMessage(id)
    }

    override suspend fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCompileResult> {
        val id = sendMessage(
            ReplCompileMessage(
                sessionId,
                replStateId,
                codeLine
            )
        )
        return readMessage(id)
    }

    // Query messages:

    class CheckCompilerIdMessage(val expectedCompilerId: CompilerId) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.checkCompilerId(expectedCompilerId))
    }

    class GetUsedMemoryMessage : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.getUsedMemory())
    }

    class GetDaemonOptionsMessage : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.getDaemonOptions())
    }

    class GetDaemonJVMOptionsMessage : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.getDaemonJVMOptions())
    }

    class GetDaemonInfoMessage : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.getDaemonInfo())
    }

    class RegisterClientMessage(val aliveFlagPath: String?) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.registerClient(aliveFlagPath))
    }


    class GetClientsMessage : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.getClients())
    }

    class LeaseCompileSessionMessage(val aliveFlagPath: String?) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.leaseCompileSession(aliveFlagPath))
    }

    class ReleaseCompileSessionMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.releaseCompileSession(sessionId))
    }

    class ShutdownMessage : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.shutdown())
    }

    class ScheduleShutdownMessage(val graceful: Boolean) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.scheduleShutdown(graceful))
    }

    class CompileMessage(
        val sessionId: Int,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBaseAsync,
        val compilationResults: CompilationResultsAsync?
    ) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(
                server.compile(
                    sessionId,
                    compilerArguments,
                    compilationOptions,
                    servicesFacade,
                    compilationResults
                )
            )
    }

    class ClassesFqNamesByFilesMessage(
        val sessionId: Int,
        val sourceFiles: Set<File>
    ) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(
                server.classesFqNamesByFiles(sessionId, sourceFiles)
            )
    }

    class ClearJarCacheMessage : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            server.clearJarCache()
    }

    class LeaseReplSessionMessage(
        val aliveFlagPath: String?,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBaseAsync,
        val templateClasspath: List<File>,
        val templateClassName: String
    ) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(
                server.leaseReplSession(
                    aliveFlagPath,
                    compilerArguments,
                    compilationOptions,
                    servicesFacade,
                    templateClasspath,
                    templateClassName
                )
            )
    }

    class ReleaseReplSessionMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.releaseReplSession(sessionId))
    }

    class LeaseReplSession_Short_Message(
        val aliveFlagPath: String?,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBase,
        val templateClasspath: List<File>,
        val templateClassName: String
    ) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(
                server.leaseReplSession(
                    aliveFlagPath,
                    compilerArguments,
                    compilationOptions,
                    servicesFacade.toClient(),
                    templateClasspath,
                    templateClassName
                )
            )
    }

    class ReplCreateStateMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.replCreateState(sessionId))
    }

    class ReplCheckMessage(
        val sessionId: Int,
        val replStateId: Int,
        val codeLine: ReplCodeLine
    ) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.replCheck(sessionId, replStateId, codeLine))
    }

    class ReplCompileMessage(
        val sessionId: Int,
        val replStateId: Int,
        val codeLine: ReplCodeLine
    ) : Server.Message<CompileServiceServerSide>() {
        override suspend fun processImpl(server: CompileServiceServerSide, sendReply: (Any?) -> Unit) =
            sendReply(server.replCompile(sessionId, replStateId, codeLine))
    }

}
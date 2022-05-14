@file:kotlin.jvm.JvmName("ProcessKt")
@file:kotlin.jvm.JvmVersion
package kotlin.system

/**
 * Terminates the currently running Java Virtual Machine. The
 * argument serves as a status code; by convention, a nonzero status
 * code indicates abnormal termination.
 *
 * This method never returns normally.
 */
public fun exitProcess(status: Int): Nothing {
    System.exit(status)
    throw RuntimeException("System.exit returned normally, while it was supposed to halt JVM.")
}
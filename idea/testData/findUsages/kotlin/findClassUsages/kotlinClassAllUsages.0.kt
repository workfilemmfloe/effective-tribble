// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: usages, constructorUsages
package server

open class <caret>Server {
    companion object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}

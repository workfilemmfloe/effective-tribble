// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: constructorUsages
package server

open class <caret>Server {
    class object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}

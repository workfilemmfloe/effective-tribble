// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetProperty
// OPTIONS: usages
package client

import server.foo

fun test() {
    foo()
    val t = 1.foo + foo
}

// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: inline.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME
// FULL_JDK
package test

class A {
    inline fun doAssert() {
        assert(false)
    }
}

// FILE: inlineSite.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
import test.*

class B {
    companion object {
        @JvmField
        val triggered: Boolean = try {
            A().doAssert()
            false
        } catch (e: AssertionError) {
            true
        }
    }
}

class Dummy

fun box(): String {
    val loader = Dummy::class.java.classLoader
    loader.setDefaultAssertionStatus(false)
    return if (loader.loadClass("B").getField("triggered").get(null) == true)
        "FAIL: assertion triggered"
    else
        "OK"
}

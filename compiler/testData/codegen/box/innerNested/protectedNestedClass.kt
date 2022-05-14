// See KT-9246 IllegalAccessError when trying to access protected nested class from parent class
// TARGET_BACKEND: JVM
// FILE: a.kt

package a

abstract class A {
    protected class C {
        fun result() = "OK"
    }
}

// FILE: b.kt

package b

import a.A

class B : A() {
    protected val c = A.C()
    val result: String get() = c.result()
}

fun box(): String {
    return B().result
}

// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

fun foo(fn: () -> Unit) = fn()

inline fun twice(fn: () -> Unit) {
    fn()
    fn()
}

// FILE: 2.kt
fun box(): String {
    var test = 0
    twice {
        foo { test += 1 }
    }
    if (test != 2)
        return "Failed: test=$test"

    return "OK"
}
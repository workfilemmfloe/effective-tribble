// IGNORE_ERRORS
// ERROR_POLICY: SEMANTIC

// MODULE: lib
// FILE: t.kt

class A
class B

fun bar(): B = B()
fun foo(): A {
    return bar()
}

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: ClassCastException) {
        return "OK"
    }
    return "FAIL"
}
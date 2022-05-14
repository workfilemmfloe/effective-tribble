// !DIAGNOSTICS_NUMBER: 5
// !DIAGNOSTICS: NESTED_CLASS_SHOULD_BE_QUALIFIED

package p

class A {
    class B {
        class Nested
    }
}

fun A.B.test() {
    Nested()
    ::Nested
}

class C {
    companion object {
        class D {
            class Nested
        }
    }
}

fun C.Companion.D.text() {
    Nested()
    ::Nested
}

class E {
    class F {
        companion object
    }
}

fun E.test() {
    F
}
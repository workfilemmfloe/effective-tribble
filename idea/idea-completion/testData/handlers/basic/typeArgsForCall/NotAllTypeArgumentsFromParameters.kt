fun <T1, reified T2> foo(t: T1): T1 {}

fun f() {
    <caret>
}

// ELEMENT: foo

fun testInvoke() {
    operator fun Nothing.invoke() = this
    todo()<!UNREACHABLE_CODE!>()<!>
}

fun testInvokeWithLambda() {
    operator fun Nothing.invoke(<!UNUSED_PARAMETER!>i<!>: Int, f: () -> Int) = f
    todo()<!UNREACHABLE_CODE!>(1){ 42 }<!>
}

fun todo(): Nothing = throw Exception()
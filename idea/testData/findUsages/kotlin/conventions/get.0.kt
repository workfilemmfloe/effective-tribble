// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

class B(val n: Int) {
    fun <caret>get(i: Int): B = B(i)
}

fun test() {
    B(1).get(2)
    B(1)[2]
}

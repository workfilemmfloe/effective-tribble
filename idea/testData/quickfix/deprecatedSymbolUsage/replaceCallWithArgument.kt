// "Replace with 'p'" "true"
@Deprecated("", ReplaceWith("p"))
fun oldFun(p: Int): Int = p

fun foo() {
    <caret>oldFun(0)
}

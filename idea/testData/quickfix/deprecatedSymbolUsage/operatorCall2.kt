// "Replace with 'newFun(p)'" "true"

interface I

@Deprecated("", ReplaceWith("newFun(p)"))
fun I.plus(p: Int) {
    newFun(p)
}

fun I.newFun(p: Int){}

fun foo(i: I) {
    i <caret>+ 1
}

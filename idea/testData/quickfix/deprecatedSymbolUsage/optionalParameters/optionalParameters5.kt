// "Replace with 'newFun(p1, p2)'" "true"

interface I {
    @deprecated("", ReplaceWith("newFun(p1, p2)"))
    fun oldFun(p1: String, p2: String = p1)

    fun newFun(p1: String, p2: String)
}

fun foo(i: I) {
    i.<caret>oldFun(bar())
}

fun bar(): String = ""
// "Replace with 'newFun(p1, p2.toString(), p3)'" "true"

interface I {
    @Deprecated("", ReplaceWith("newFun(p1, p2.toString(), p3)"))
    fun oldFun(p1: String, p2: Int = 0, p3: String? = null)

    fun newFun(p1: String, p2: String, p3: String? = null)
}

fun foo(i: I) {
    i.<caret>oldFun("a")
}

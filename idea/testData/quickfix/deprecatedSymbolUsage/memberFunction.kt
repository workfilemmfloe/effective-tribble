// "Replace with 'newFun()'" "true"

class X {
    @deprecated("", ReplaceWith("newFun()"))
    fun oldFun() {
        newFun()
    }

    fun newFun(){}
}

fun foo(x: X) {
    x.<caret>oldFun()
}

// "Replace with 'newFun(*p, 1)'" "true"
// WITH_RUNTIME

@Deprecated("", ReplaceWith("newFun(*p, 1)"))
fun oldFun(vararg p: Int){
    newFun(*p, 1)
}

fun newFun(vararg p: Int){}

fun foo() {
    <caret>oldFun(1, 2, 3)
}

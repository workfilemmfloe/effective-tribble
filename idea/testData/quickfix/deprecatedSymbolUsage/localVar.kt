// "Replace with '1'" "true"

fun foo(): Int {
    @Deprecated("", ReplaceWith("1"))
    val localVar = 1

    return <caret>localVar
}
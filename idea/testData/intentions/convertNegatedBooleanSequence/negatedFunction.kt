fun bar1(): Boolean {
    return true
}

fun bar2(): Boolean {
    return false
}

fun foo(): Boolean {
    return <caret>!bar1() && !bar2()
}

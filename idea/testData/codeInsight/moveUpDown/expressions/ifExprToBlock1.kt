// MOVE: up

fun foo(x: Boolean) {
    if (x) {

    }
    val p = <caret>if (x) {
        0
    }
    else {
        1
    }
}

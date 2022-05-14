// "Replace with safe (?.) call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Flip '<'
// ACTION: Replace overloaded operator with function call
// ERROR: Infix call corresponds to a dot-qualified call 'w?.x.compareTo(42)' which is not allowed on a nullable receiver 'w?.x'. Use '?.'-qualified call instead

class Wrapper(val x: Int)

fun test(w: Wrapper?) {
    if (w?.x <caret>< 42) {}
}
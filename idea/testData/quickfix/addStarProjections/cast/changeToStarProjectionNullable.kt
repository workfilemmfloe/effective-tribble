// "Change type arguments to <*, *>" "true"
public fun foo(a: Any?) {
    a as Map<*, Int>?<caret>
}

// "Add '<*, *>'" "true"
public fun foo(a: Any) {
    when (a) {
        is jet.Map<caret> -> {}
        else -> {}
    }
}
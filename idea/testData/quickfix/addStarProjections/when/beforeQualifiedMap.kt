// "Add '<*, *>'" "true"
public fun foo(a: Any) {
    when (a) {
        is kotlin.Map<caret> -> {}
        else -> {}
    }
}
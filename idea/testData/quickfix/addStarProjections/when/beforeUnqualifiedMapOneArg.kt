// "class org.jetbrains.jet.plugin.quickfix.AddStarProjectionsFix" "false"
// ERROR: 2 type arguments expected
public fun foo(a: Any) {
    when (a) {
        is <caret>Map<Int> -> {}
        else -> {}
    }
}
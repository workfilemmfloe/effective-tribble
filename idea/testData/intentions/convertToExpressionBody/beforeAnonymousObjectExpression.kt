// "Convert to expression body" "true"
trait I {
    fun foo(): String
}

fun bar(): I {
    <caret>return object: I {
        override fun foo(): String {
            return "a"
        }
    }
}
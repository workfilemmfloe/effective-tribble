trait I {
    public fun getString(): String
}

class C() {
    fun foo(i: I, b: Boolean) {
        val result = i.getString()
        if (b) result = null
        if (result != null) {
            print(result)
        }
    }
}
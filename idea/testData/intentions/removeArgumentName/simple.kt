fun foo(s: String, b: Boolean){}

fun bar() {
    foo("", <caret>b = true)
}
fun foo(vararg args: Int, handler: (String, Char) -> Unit){}

fun bar(handler: (String, Char) -> Unit) {
    foo() <caret>
}

// EXIST: "{ String, Char -> ... }"
// ABSENT: handler
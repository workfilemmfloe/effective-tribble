fun foo(p: () -> Unit, i: Int){}

fun bar() {
    foo(<caret>)
}

// ELEMENT: "{...}"

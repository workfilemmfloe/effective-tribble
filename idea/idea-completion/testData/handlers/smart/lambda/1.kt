fun foo(p: () -> Unit){}

fun bar() {
    foo(<caret>)
}

// ELEMENT: "{...}"

fun foo(s: String){ }

fun bar(sss: String) {
    foo(<caret>x().y.z())
}

//ELEMENT: sss
//CHAR: \t

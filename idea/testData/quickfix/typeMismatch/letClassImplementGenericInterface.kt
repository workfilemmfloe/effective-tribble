// "Let 'B' implement interface 'A<Int>'" "true"
package let.implement

fun bar() {
    foo(B()<caret>)
}


fun foo(a: A<Int>) {
}

interface A<T>
class B
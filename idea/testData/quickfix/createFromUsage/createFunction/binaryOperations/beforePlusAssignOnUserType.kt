// "Create function 'plusAssign'" "true"

class A<T>(val n: T)

fun test() {
    A(1) <caret>+= 2
}
class A {
    fun component1() = 1
    fun component2() = 1
}

class C {
    fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((<!REDECLARATION!>x<!>, <!NAME_SHADOWING, REDECLARATION!>x<!>) in C()) {

    }
}
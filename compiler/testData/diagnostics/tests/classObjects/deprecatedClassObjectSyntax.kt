trait G

class A {
    <!DEPRECATED_CLASS_OBJECT_SYNTAX!>class object A<!> {

    }
}

trait B {
    <!DEPRECATED_CLASS_OBJECT_SYNTAX!>class object<!> : G {

    }
}

class C {
    companion <!DEPRECATED_CLASS_OBJECT_SYNTAX!>class object<!>
}

fun main() {
    A
    A.A
    g(B.Companion)
    g(B)

    A.ext()
    A.A.ext()
}

fun g(g: G) { <!UNUSED_EXPRESSION!>g<!> }

fun A.A.ext() {
}
package test

import test.A.C

class A {
    class <caret>C {

    }
}

class B {
    val x = A.C()
}
open class A {
    private val a = B()

    protected class <caret>B {
        private val c = C()
    }

    protected class C()
}

class X : A() {
    private val b = B()
}
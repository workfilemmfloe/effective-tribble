// "Create function 'foo'" "true"
// ERROR: Unresolved reference: B

class A: B {
    fun foo() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

fun test() {
    A().foo()
}
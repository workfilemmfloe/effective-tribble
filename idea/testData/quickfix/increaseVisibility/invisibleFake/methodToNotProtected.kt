// "Make 'doSth' protected" "false"
// ACTION: Make 'doSth' public
// ACTION: Make 'doSth' internal
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Cannot access 'doSth': it is private in 'A'

class A {
    private fun doSth() {
    }
}

class B {
    fun bar() {
        A().<caret>doSth()
    }
}
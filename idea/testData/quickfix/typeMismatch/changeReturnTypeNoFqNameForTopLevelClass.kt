// "Change 'A.foo' function return type to 'Int'" "true"
package foo.bar

class A {
    fun foo(): String {
        return <caret>1
    }
}


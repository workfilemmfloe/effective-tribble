//ALLOW_AST_ACCESS
package test

annotation class Anno

class Class {
    fun String.foo(@[Anno] x: Int) = 42
}

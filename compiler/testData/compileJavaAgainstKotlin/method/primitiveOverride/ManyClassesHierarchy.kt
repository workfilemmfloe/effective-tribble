package test

trait A<T> {
    fun foo(): T
}

trait B : A<Int>

abstract class C : B

open class D : C() {
    override fun foo(): Int = 42
}

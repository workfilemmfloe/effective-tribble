// "Change function signature to 'fun f(a: Int)'" "true"
interface A {
    fun f(a: Int)
}

interface B : A {
    <caret>override fun f(a: String)
}

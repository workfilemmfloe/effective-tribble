// "Cast expression 'x' to 'Foo<out Number>'" "true"
trait Foo<T: Number> {
    fun foo()
}

fun bar(_x: Any) {
    var x = _x
    if (x is Foo<*>) {
        (x as Foo<out Number>)<caret>.foo()
    }
}
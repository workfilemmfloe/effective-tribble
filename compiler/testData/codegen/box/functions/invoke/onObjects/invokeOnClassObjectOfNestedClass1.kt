class A {
    class Nested {
        class object {
            fun invoke(i: Int) = i
        }
    }
}

fun box() = if (A.Nested(42) == 42) "OK" else "fail"

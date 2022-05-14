import utils.*

// CHECK_CONTAINS_NO_CALLS: test

internal class A(val n: Int)

internal fun test(a: A, m: Int): Int = apply(a) { n * m }

fun box(): String {
    assertEquals(6, test(A(2), 3))

    return "OK"
}
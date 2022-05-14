import utils.*

// CHECK_CONTAINS_NO_CALLS: test

fun multiplyBy2(x: Int): Int = x * 2

fun test(x: Int): Int = apply(x, ::multiplyBy2)

fun box(): String {
    assertEquals(6, test(3))

    return "OK"
}
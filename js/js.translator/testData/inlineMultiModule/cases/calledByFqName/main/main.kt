// CHECK_CONTAINS_NO_CALLS: test

fun test(x: Int, y: Int): Int = utils.sum(x, y)

fun box(): String {
    assertEquals(3, test(1, 2))
    assertEquals(5, test(2, 3))

    return "OK"
}
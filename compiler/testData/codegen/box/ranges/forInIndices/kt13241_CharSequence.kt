// WITH_STDLIB

import kotlin.test.assertEquals

fun test(x: Any): Int {
    var sum = 0
    if (x is String) {
        for (i in x.indices) {
            sum = sum * 10 + i
        }
    }
    return sum
}

fun box(): String {
    assertEquals(123, test("0000"))
    return "OK"
}
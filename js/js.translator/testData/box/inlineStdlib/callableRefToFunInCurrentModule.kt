// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1525
package foo

// CHECK_NOT_CALLED_IN_SCOPE: scope=test function=even TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: scope=test function=filter TARGET_BACKENDS=JS

internal inline fun even(x: Int) = x % 2 == 0

internal fun test(a: List<Int>) = a.filter(::even)

fun box(): String {
    assertEquals(listOf(2, 4), test(listOf(1, 2, 3, 4)))

    return "OK"
}
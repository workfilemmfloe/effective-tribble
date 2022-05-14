// EXPECTED_REACHABLE_NODES: 998
package foo

fun test(a: Any?) = a.toString()

fun box(): String {
    arrayOf(1, 2, *arrayOf(1, 2)).size
    val a = "O" + "K"
    a != a
    null == null
    return test(a)
}

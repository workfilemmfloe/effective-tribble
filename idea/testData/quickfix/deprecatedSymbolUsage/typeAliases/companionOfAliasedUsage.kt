// "Replace with 'С'" "true"
package ppp

@Deprecated("", replaceWith = ReplaceWith("C"))
private typealias A = C
private class C {
    companion object {
        val compVal = 1
    }
}

fun f() {
    val v2 = <caret>A.compVal
}
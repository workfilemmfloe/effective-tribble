fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    if (x != null) {
        when (x) {
            0 -> bar(<!DEBUG_INFO_AUTOCAST!>x<!>)
            else -> {}
        }
    }

    when (x) {
        0 -> { if (<!SENSELESS_COMPARISON!>x == null<!>) return }
        else -> { if (x == null) return }
    }
    bar(<!DEBUG_INFO_AUTOCAST!>x<!>)
}

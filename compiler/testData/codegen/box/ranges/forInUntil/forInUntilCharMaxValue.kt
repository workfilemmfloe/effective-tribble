// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    for (i in Char.MAX_VALUE until Char.MAX_VALUE) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}

package a

import a.Test as _Test
import a.test as _Test
import a.TEST as _TEST

fun bar() {
    val t: _Test = _Test()
    _test()
    t._test()
    println(_TEST)
    println(t._TEST)
    _TEST = ""
    t._TEST = ""
}

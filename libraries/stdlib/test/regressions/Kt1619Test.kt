package regressions

import junit.framework.TestCase

class Kt1619Test: TestCase() {

    fun doSomething(list: List<String?>): Boolean {
        return list.size() > 0
    }

    fun testCollectionNotNullCanBeUsedForNullables() {
        val list: List<String> = arrayList("foo", "bar")
        // TODO uncomment this line to reproduce KT-1619
        // doSomething(list)
    }
}
interface I

class C

class O

class E {
    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }
}

open class B {
    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }
}

class BB : B()

class X {
    fun foo(i1: I?, i2: I?, s1: String, s2: String, c1: C, c2: C, i: Int, o1: O, o2: O, e1: E, e2: E, bb1: BB, bb2: BB, arr1: IntArray, arr2: IntArray) {
        if (i1 === i2) return
        if (s1 === s2) return
        if (c1 == c2) return
        if (i1 == null) return
        if (null == i2) return
        if (i == 0) return
        if (o1 === o2) return
        if (e1 === e2) return
        if (bb1 === bb2) return
        if (arr1 == arr2) return

        if (s1 !== s2) return
        if (c1 != c2) return
    }
}

// ERROR: Type mismatch: inferred type is kotlin.Any? but kotlin.Any was expected
// ERROR: Type mismatch: inferred type is kotlin.Any? but kotlin.Any was expected
class A {
    JvmOverloads fun foo(s: String? = null): Any {
        println("s = " + s!!)
        return ""
    }

    fun bar(s: String?): Any? {
        println("s = " + s!!)
        return if (s == null) "" else null
    }

    fun bar(): Any {
        return bar(null)
    }

    public fun bar1(s: String?): Any? {
        println("s = " + s!!)
        return if (s == null) "" else null
    }

    public fun bar1(): Any {
        return bar1(null)
    }

    Deprecated("")
    public fun f() {
        f(1)
    }

    public fun f(p: Int) {
        println("p = " + p)
    }
}

class Delegate {
    var inner = Derived()
    fun getValue(t: Any?, p: PropertyMetadata): Derived {
        inner = Derived(inner.a + "-get")
        return inner
    }
    fun setValue(t: Any?, p: PropertyMetadata, i: Base) { inner = Derived(inner.a + "-" + i.a + "-set") }
}

class A {
    var prop: Derived by Delegate()
}

fun box(): String {
    val c = A()
    if(c.prop.a != "derived-get") return "fail get ${c.prop.a}"
    c.prop = Derived()
    if (c.prop.a != "derived-get-derived-set-get") return "fail set ${c.prop.a}"
    return "OK"
}

open class Base(open val a: String = "base")

class Derived(override val a: String = "derived"): Base()

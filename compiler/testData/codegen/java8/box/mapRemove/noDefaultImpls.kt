// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS
// FULL_JDK

// There should be no DefaultImpls method for MutableMap.remove(K;V)
interface A<K, V> : MutableMap<K, V>

class B : A<String, String>, java.util.AbstractMap<String, String>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = java.util.HashSet()
}

interface C<K, V> : MutableMap<K, V> {
    override fun remove(key: K, value: V) = true
}

class D : A<String, String>, java.util.AbstractMap<String, String>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = java.util.HashSet()
}

fun box(): String {
    val x1 = B().remove("1", "2")
    if (x1) return "fail 1"

    val x2 = D().remove("3", "4")
    if (x1) return "fail 2"

    return "OK"
}

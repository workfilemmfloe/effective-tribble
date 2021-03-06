// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: Test.java

public class Test {
    public static String invokeMethodWithOverloads() {
        C<String> c = new C<String>();
        return c.foo("O");
    }
}

// FILE: generics.kt

class C<T> {
    @kotlin.jvm.JvmOverloads public fun foo(o: T, k: String = "K"): String = o.toString() + k
}

fun box(): String {
    return Test.invokeMethodWithOverloads()
}

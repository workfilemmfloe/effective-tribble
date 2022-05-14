// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A {
    void foo(List<Object> x) {}
}
// FILE: main.kt

fun main(a: A, ml: Any) {
    if (ml is <!CANNOT_CHECK_FOR_ERASED!>MutableList<String><!>) {
        a.foo(<!JAVA_TYPE_MISMATCH, DEBUG_INFO_SMARTCAST!>ml<!>)
        a.foo(<!UNCHECKED_CAST!>ml as List<Any><!>)
    }
}

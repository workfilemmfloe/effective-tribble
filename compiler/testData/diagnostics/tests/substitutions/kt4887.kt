package h

public class MyClass<S, T>(<!UNUSED_PARAMETER!>param<!>: MyClass<S, T>) {
    fun test() {
        val result: MyClass<Any, Any>? = null
        MyClass<S, Any>(result <!CAST_NEVER_SUCCEEDS!>as<!> MyClass<S, Any>)
    }
}
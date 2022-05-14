// !CHECK_TYPE
// !LANGUAGE: -AdditionalBuiltInsMembers
// SKIP_TXT

class A : java.util.ArrayList<String>() {
    override fun stream(): java.util.stream.Stream<String> = super.stream()
}

class A1 : java.util.ArrayList<String>() {
    fun stream(): java.util.stream.Stream<String> = super.stream()
}

interface A2 : List<String> {
    <!UNSUPPORTED_FEATURE!>override<!> fun stream(): java.util.stream.Stream<String> = null!!
}

class B : <!UNSUPPORTED_FEATURE!>Throwable<!>("", null, false, false)

fun Throwable.<!EXTENSION_SHADOWED_BY_MEMBER!>fillInStackTrace<!>() = 1

fun foo(x: List<String>, y: Throwable) {
    x.<!UNSUPPORTED_FEATURE!>stream<!>()
    java.util.ArrayList<String>().stream()

    y.fillInStackTrace() checkType { _<Int>() }

    HashMap<String, Int>().getOrDefault(Any(), null)

    // Falls back to extension in stdlib
    y.printStackTrace()
}

interface X {
    fun foo(): Int = 1
    val hidden: Boolean
}

class Y : X {
    // There should not be UNSUPPORTED_FEATURE diagnostic
    override fun foo() = 1
    override var hidden: Boolean = true
}

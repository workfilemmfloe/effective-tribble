package h

fun foo(i: Int) = i
fun foo(s: String) = s

fun test() {
    <!NONE_APPLICABLE!>foo<!>(<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>())
}

fun emptyList<T>(): List<T> {throw Exception()}
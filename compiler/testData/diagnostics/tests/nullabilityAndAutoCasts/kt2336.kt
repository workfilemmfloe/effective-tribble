fun main(args: Array<String>) {
    val b: Boolean? = null
    if (b != null) {
        if (!<!DEBUG_INFO_AUTOCAST!>b<!>) {} // OK
        if (<!DEBUG_INFO_AUTOCAST!>b<!>) {} // Error: Condition must be of type jet.Boolean, but is of type jet.Boolean?
        if (b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {} // WARN: Unnecessary non-null assertion (!!) on a non-null receiver of type jet.Boolean?
        foo(<!DEBUG_INFO_AUTOCAST!>b<!>) // OK
    }
}

fun foo(a: Boolean) = a
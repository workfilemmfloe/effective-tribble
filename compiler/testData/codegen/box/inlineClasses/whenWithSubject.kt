// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineLong(val value: Long)
inline val Number.toInlineLong get() = InlineLong(this.toLong())

fun box(): String {
    val value = 0

    val withoutSubject = when (value.toInlineLong) {
        0.toInlineLong -> true
        else -> false
    }
    if (!withoutSubject) return "Fail: without subject"

    val withSubject = when (val subject = value.toInlineLong) {
        0.toInlineLong -> true
        else -> false
    }
    if (!withSubject) return "Fail: with subject"

    return "OK"
}

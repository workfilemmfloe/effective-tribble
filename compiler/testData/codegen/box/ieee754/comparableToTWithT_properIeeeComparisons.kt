// !LANGUAGE: +ProperIeee754Comparisons
// IGNORE_BACKEND: JS_IR

fun less(x: Comparable<Float>, y: Float) = x is Float && x < y
fun less(x: Comparable<Double>, y: Double) = x is Double && x < y

fun box(): String {
    if (less(-0.0F, 0.0F)) return "Fail F"
    if (less(-0.0, 0.0)) return "Fail D"

    return "OK"
}
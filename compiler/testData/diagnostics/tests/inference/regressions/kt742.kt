//KT-742 Stack overflow in type inference
package a

fun <T : Any> T?.sure() : T = this!!

class List<T>(val head: T, val tail: List<T>? = null)

fun <T, Q> List<T>.map1(f: (T)-> Q): List<T>? = tail!!.map1(f)

fun <T, Q> List<T>.map2(f: (T)-> Q): List<T>? = <!TYPE_MISMATCH!>tail<!>.sure<T>().<!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>map2<!>(f)
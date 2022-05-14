//KT-2445 Calling method with function with generic parameter causes compile-time exception
package a

fun main(args: Array<String>) {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!> {

    }
}

fun test<R>(callback: (R) -> Unit):Unit = callback(null!!)

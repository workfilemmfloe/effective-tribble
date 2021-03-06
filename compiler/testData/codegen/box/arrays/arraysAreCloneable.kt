// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun foo(x: Cloneable) = x

fun box(): String {
    foo(arrayOf(""))
    foo(intArrayOf())
    foo(longArrayOf())
    foo(shortArrayOf())
    foo(byteArrayOf())
    foo(charArrayOf())
    foo(doubleArrayOf())
    foo(floatArrayOf())
    foo(booleanArrayOf())

    arrayOf("").clone()
    intArrayOf().clone()
    longArrayOf().clone()
    shortArrayOf().clone()
    byteArrayOf().clone()
    charArrayOf().clone()
    doubleArrayOf().clone()
    floatArrayOf().clone()
    booleanArrayOf().clone()

    return "OK"
}

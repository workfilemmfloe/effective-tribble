// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

@Retention(AnnotationRetention.RUNTIME)
annotation class Simple(val value: String)

@Simple("OK")
fun box(): String {
    return (::box.annotations.single() as Simple).value
}

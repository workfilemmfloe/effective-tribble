// WITH_STDLIB

fun box(): String {
    var methodVar = "OK"

    fun localMethod() : String
    {
        return lazy { methodVar }::value.get()
    }

    return localMethod()
}
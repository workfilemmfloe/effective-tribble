// WITH_STDLIB

fun box(): String {
    val map: Map<String, Boolean>? = mapOf()
    return if (map?.get("") == true) "fail" else "OK"
}
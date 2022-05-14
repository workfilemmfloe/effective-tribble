package kotlin.js

public external val noImpl: Nothing
    get() = noImpl

public external fun eval(expr: String): dynamic = noImpl

public external val undefined: Nothing? = noImpl


@library
public fun println() {}

@library
public fun println(s: Any?) {}

@library
public fun print(s: Any?) {}

//TODO: consistent parseInt
public external fun parseInt(s: String, radix: Int = noImpl): Int = noImpl

@library
public fun safeParseInt(s: String): Int? = noImpl

@library
public fun safeParseDouble(s: String): Double? = noImpl

public external fun js(code: String): dynamic = noImpl

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
public inline fun jsTypeOf(a: Any?): String = js("typeof a")

@library
internal fun deleteProperty(`object`: Any, property: Any): Unit = noImpl
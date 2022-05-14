package kotlin.js

@Deprecated(message = "Use `definedExternally` instead", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("definedExternally"))
public external val noImpl: Nothing

public external val definedExternally: Nothing

public external fun eval(expr: String): dynamic

public external val undefined: Nothing?

@Deprecated("Use toInt(radix) instead.", ReplaceWith("s.toInt(radix)"), level = DeprecationLevel.ERROR)
public external fun parseInt(s: String, radix: Int = definedExternally): Int

@Deprecated("Use toDouble() instead.", ReplaceWith("s.toDouble()"), level = DeprecationLevel.ERROR)
public external fun parseFloat(s: String, radix: Int = definedExternally): Double

public external fun js(code: String): dynamic

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
@kotlin.internal.InlineOnly
public inline fun jsTypeOf(a: Any?): String = js("typeof a")

@kotlin.internal.InlineOnly
internal inline fun deleteProperty(obj: Any, property: Any) {
    js("delete obj[property]")
}
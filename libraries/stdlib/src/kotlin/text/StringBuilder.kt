@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

/**
 * Builds newly created StringBuilder using provided body.
 */
@Deprecated("Use StringBuilder().apply { body } or use buildString { body } if you need String as a result.", ReplaceWith("StringBuilder().apply(body)"))
public inline fun StringBuilder(body: StringBuilder.() -> Unit): StringBuilder = StringBuilder().apply(body)

/**
 * Builds new string by populating newly created [StringBuilder] using provided [builderAction] and then converting it to [String].
 */
public inline fun buildString(builderAction: StringBuilder.() -> Unit): String = StringBuilder().apply(builderAction).toString()

/**
 * Appends all arguments to the given [Appendable].
 */
public fun <T : Appendable> T.append(vararg value: CharSequence?): T {
    for (item in value)
        append(item)
    return this
}

/**
 * Appends all arguments to the given StringBuilder.
 */
public fun StringBuilder.append(vararg value: String?): StringBuilder {
    for (item in value)
        append(item)
    return this
}

/**
 * Appends all arguments to the given StringBuilder.
 */
public fun StringBuilder.append(vararg value: Any?): StringBuilder {
    for (item in value)
        append(item)
    return this
}

/**
 * Sets the character at the specified [index] to the specified [value].
 */
@kotlin.jvm.JvmVersion
public operator fun StringBuilder.set(index: Int, value: Char): Unit = this.setCharAt(index, value)

package kotlin

import java.util.ArrayList
import kotlin.platform.platformName

/**
 * Returns a single list of all elements from all collections in the given collection.
 */
public fun <T> Iterable<Iterable<T>>.flatten(): List<T> {
    val result = ArrayList<T>()
    for (element in this) {
        result.addAll(element)
    }
    return result
}

/**
 * Returns a sequence of all elements from all sequences in this sequence.
 */
public fun <T> Sequence<Sequence<T>>.flatten(): Sequence<T> {
    return MultiSequence(this)
}

/**
 * Returns a single list of all elements from all arrays in the given array.
 */
public fun <T> Array<Array<out T>>.flatten(): List<T> {
    val result = ArrayList<T>(sumBy { it.size() })
    for (element in this) {
        result.addAll(element)
    }
    return result
}


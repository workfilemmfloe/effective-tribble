// WITH_RUNTIME
// PARAM_TYPES: Foo<T>
// PARAM_TYPES: kotlin.String
// PARAM_DESCRIPTOR: internal final class Foo<T> defined in root package
// PARAM_DESCRIPTOR: value-parameter l: kotlin.String defined in Foo.test

import java.util.*

// SIBLING:
internal class Foo<T> {
    val map = HashMap<String, T>()

    fun test(l: String): T {
        return <selection>map[l]</selection>
    }
}
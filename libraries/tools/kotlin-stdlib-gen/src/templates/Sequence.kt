package templates

import templates.Family.*

fun sequences(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("asIterable()") {
        only(Iterables, ArraysOfObjects, ArraysOfPrimitives, Sequences, CharSequences, Maps)
        doc { f -> "Creates an [Iterable] instance that wraps the original ${f.collection} returning its ${f.element.pluralize()} when being iterated." }
        returns("Iterable<T>")
        body { f ->
            """
            ${ when(f) {
                ArraysOfObjects, ArraysOfPrimitives -> "if (isEmpty()) return emptyList()"
                CharSequences -> "if (this is String && isEmpty()) return emptyList()"
                else -> ""
            }}
            return object : Iterable<T> {
                override fun iterator(): Iterator<T> = this@asIterable.iterator()
            }
            """
        }

        inline(Iterables, Maps) { Inline.Only }

        doc(Iterables) { "Returns this collection as an [Iterable]." }
        body(Iterables) { "return this" }
        body(Maps) { "return entries" }
    }

    templates add f("asSequence()") {
        include(Maps)
        doc { f -> "Creates a [Sequence] instance that wraps the original ${f.collection} returning its ${f.element.pluralize()} when being iterated." }
        returns("Sequence<T>")
        body { f ->
            """
            ${ when(f) {
                ArraysOfObjects, ArraysOfPrimitives -> "if (isEmpty()) return emptySequence()"
                CharSequences -> "if (this is String && isEmpty()) return emptySequence()"
                else -> ""
            }}
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    return this@asSequence.iterator()
                }
            }
            """
        }

        // TODO: Drop special case

        body(CharSequences) {
            """
            if (this is String && isEmpty()) return emptySequence()
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    return this@asSequence.iterator()
                }
            }
            """
        }

        doc(Sequences) { "Returns this sequence as a [Sequence]."}
        inline(Sequences) { Inline.Only }
        body(Sequences) { "return this" }
    }

    return templates
}


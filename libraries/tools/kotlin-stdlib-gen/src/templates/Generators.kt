package templates

import templates.Family.*

fun generators(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("plus(element: T)") {
        doc { "Returns a list containing all elements of original collection and then the given element" }
        returns("List<T>")
        body {
            """
                val answer = toArrayList()
                answer.add(element)
                return answer
            """
        }

        doc(Streams) { "Returns a stream containing all elements of original stream and then the given element" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            return Multistream(streamOf(this, streamOf(element)))
            """
        }
    }

    templates add f("plus(collection: Iterable<T>)") {
        exclude(Streams)
        doc { "Returns a list containing all elements of original collection and then all elements of the given *collection*" }
        returns("List<T>")
        body {
            """
                val answer = toArrayList()
                answer.addAll(collection)
                return answer
            """
        }
    }

    templates add f("plus(array: Array<T>)") {
        exclude(Streams)
        doc { "Returns a list containing all elements of original collection and then all elements of the given *collection*" }
        returns("List<T>")
        body {
            """
                val answer = toArrayList()
                answer.addAll(array)
                return answer
            """
        }
    }

    templates add f("plus(collection: Iterable<T>)") {
        only(Streams)
        doc { "Returns a stream containing all elements of original stream and then all elements of the given *collection*" }
        returns("Stream<T>")
        body {
            """
            return Multistream(streamOf(this, collection.stream()))
            """
        }
    }

    templates add f("plus(stream: Stream<T>)") {
        only(Streams)
        doc { "Returns a stream containing all elements of original stream and then all elements of the given *stream*" }
        returns("Stream<T>")
        body {
            """
            return Multistream(streamOf(this, stream))
            """
        }
    }

    templates add f("partition(predicate: (T) -> Boolean)") {
        inline(true)

        doc {
            """
            Splits original collection into pair of collections,
            where *first* collection contains elements for which predicate yielded *true*,
            while *second* collection contains elements for which predicate yielded *false*
            """
        }
        // TODO: Stream variant
        returns("Pair<List<T>, List<T>>")
        body {
            """
                val first = ArrayList<T>()
                val second = ArrayList<T>()
                for (element in this) {
                    if (predicate(element)) {
                        first.add(element)
                    } else {
                        second.add(element)
                    }
                }
                return Pair(first, second)
            """
        }
    }

    templates add f("zip(collection: Iterable<R>)") {
        exclude(Streams)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        typeParam("R")
        returns("List<Pair<T,R>>")
        body {
            """
                val first = iterator()
                val second = collection.iterator()
                val list = ArrayList<Pair<T,R>>()
                while (first.hasNext() && second.hasNext()) {
                    list.add(first.next() to second.next())
                }
                return list
            """
        }
    }

    templates add f("zip(array: Array<R>)") {
        exclude(Streams)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        typeParam("R")
        returns("List<Pair<T,R>>")
        body {
            """
                val first = iterator()
                val second = array.iterator()
                val list = ArrayList<Pair<T,R>>()
                while (first.hasNext() && second.hasNext()) {
                    list.add(first.next() to second.next())
                }
                return list
            """
        }
    }

    templates add f("zip(stream: Stream<R>)") {
        only(Streams)
        doc {
            """
            Returns a stream of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        typeParam("R")
        returns("Stream<Pair<T,R>>")
        body {
            """
                return ZippingStream(this, stream)
            """
        }
    }

    return templates
}
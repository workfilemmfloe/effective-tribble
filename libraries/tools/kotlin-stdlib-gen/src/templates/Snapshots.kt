package templates

import templates.Family.*

fun snapshots(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toCollection(collection: C)") {
        doc { "Appends all elements to the given *collection*" }
        returns("C")
        typeParam("C : MutableCollection<in T>")
        body {
            """
            for (item in this) {
                collection.add(item)
            }
            return collection
            """
        }
    }

    templates add f("toSet()") {
        doc { "Returns a Set of all elements" }
        returns("Set<T>")
        body { "return toCollection(LinkedHashSet<T>())" }
    }

    templates add f("toHashSet()") {
        doc { "Returns a HashSet of all elements" }
        returns("HashSet<T>")
        body { "return toCollection(HashSet<T>())" }
    }

    templates add f("toSortedSet()") {
        doc { "Returns a SortedSet of all elements" }
        returns("SortedSet<T>")
        body { "return toCollection(TreeSet<T>())" }
    }

    templates add f("toArrayList()") {
        doc { "Returns an ArrayList of all elements" }
        returns("ArrayList<T>")
        body { "return toCollection(ArrayList<T>())" }

        // ISSUE: JavaScript can't perform this operation
        /*
                body(Collections) {
                    """
                    return ArrayList<T>(this)
                    """
                }
        */
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val list = ArrayList<T>(size)
            for (item in this) list.add(item)
            return list
            """
        }
    }

    templates add f("toList()") {
        only(Maps)
        doc { "Returns a List containing all key-value pairs" }
        returns("List<Map.Entry<K, V>>")
        body {
            """
            val result = ArrayList<Map.Entry<K, V>>(size)
            for (item in this)
                result.add(item)
            return result
            """
        }
    }

    templates add f("toList()") {
        doc { "Returns a List containing all elements" }
        returns("List<T>")
        body { "return toCollection(ArrayList<T>())" }

        // ISSUE: JavaScript can't perform this operations
        /*
                body(Collections) {
                    """
                    return ArrayList<T>(this)
                    """
                }
                body(ArraysOfObjects) {
                    """
                    return ArrayList<T>(Arrays.asList(*this))
                    """
                }
        */
        body(ArraysOfPrimitives) {
            """
            val list = ArrayList<T>(size)
            for (item in this) list.add(item)
            return list
            """
        }
    }

    templates add f("toLinkedList()") {
        doc { "Returns a LinkedList containing all elements" }
        returns("LinkedList<T>")
        body { "return toCollection(LinkedList<T>())" }
    }

    return templates
}
package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class MutableCollectionTest {

    test fun fromIterable() {
        val data: Iterable<String> = listOf("foo", "bar")

        val collection = ArrayList<String>()
        collection.addAll(data)

        assertEquals(data, collection)
    }

    test fun fromSequence() {
        val list = listOf("foo", "bar")
        val collection = ArrayList<String>()

        collection.addAll(list.asSequence())

        assertEquals(list, collection)
    }

}

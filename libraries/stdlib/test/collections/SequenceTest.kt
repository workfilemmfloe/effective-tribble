package test.collections

import org.junit.Test as test
import kotlin.test.*
import java.util.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    var index = 0;
    var a = 0;
    var b = 1
    return sequence {
        when (index++) { 0 -> a; 1 -> b; else -> {
            val result = a + b; a = b; b = result; result
        }
        }
    }
}

public class SequenceTest {

    test fun filterEmptySequence() {
        for (sequence in listOf(emptySequence<String>(), sequenceOf<String>())) {
            assertEquals(0, sequence.filter { false }.count())
            assertEquals(0, sequence.filter { true }.count())
        }
    }

    test fun mapEmptySequence() {
        for (sequence in listOf(emptySequence<String>(), sequenceOf<String>())) {
            assertEquals(0, sequence.map { true }.count())
        }
    }

    test fun requireNoNulls() {
        val sequence = sequenceOf<String?>("foo", "bar")
        val notNull = sequence.requireNoNulls()
        assertEquals(listOf("foo", "bar"), notNull.toList())

        val sequenceWithNulls = sequenceOf("foo", null, "bar")
        val notNull2 = sequenceWithNulls.requireNoNulls() // shouldn't fail yet
        fails {
            // should throw an exception as we have a null
            notNull2.toList()
        }
    }

    test fun filterNullable() {
        val data = sequenceOf(null, "foo", null, "bar")
        val filtered = data.filter { it == null || it == "foo" }
        assertEquals(listOf(null, "foo", null), filtered.toList())
    }

    test fun filterNot() {
        val data = sequenceOf(null, "foo", null, "bar")
        val filtered = data.filterNot { it == null }
        assertEquals(listOf("foo", "bar"), filtered.toList())
    }

    test fun filterNotNull() {
        val data = sequenceOf(null, "foo", null, "bar")
        val filtered = data.filterNotNull()
        assertEquals(listOf("foo", "bar"), filtered.toList())
    }

    test fun mapNotNull() {
        val data = sequenceOf(null, "foo", null, "bar")
        val foo = data.mapNotNull { it.length() }
        assertEquals(listOf(3, 3), foo.toList())

        assertTrue {
            foo is Sequence<Int>
        }
    }

    test fun mapAndJoinToString() {
        assertEquals("3, 5, 8", fibonacci().withIndex().filter { it.index > 3 }.take(3).joinToString { it.value.toString() })
    }

    test fun withIndex() {
        val data = sequenceOf("foo", "bar")
        val indexed = data.withIndex().map { it.value.substring(0..it.index) }.toList()
        assertEquals(2, indexed.size())
        assertEquals(listOf("f", "ba"), indexed)
    }

    test fun filterAndTakeWhileExtractTheElementsWithinRange() {
        assertEquals(listOf(144, 233, 377, 610, 987), fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.toList())
    }

    test fun foldReducesTheFirstNElements() {
        val sum = { a: Int, b: Int -> a + b }
        assertEquals(listOf(13, 21, 34, 55, 89).fold(0, sum), fibonacci().filter { it > 10 }.take(5).fold(0, sum))
    }

    test fun takeExtractsTheFirstNElements() {
        assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13, 21, 34), fibonacci().take(10).toList())
    }

    test fun mapAndTakeWhileExtractTheTransformedElements() {
        assertEquals(listOf(0, 3, 3, 6, 9, 15), fibonacci().map { it * 3 }.takeWhile { i: Int -> i < 20 }.toList())
    }

    test fun joinConcatenatesTheFirstNElementsAboveAThreshold() {
        assertEquals("13, 21, 34, 55, 89, ...", fibonacci().filter { it > 10 }.joinToString(separator = ", ", limit = 5))
    }

    test fun drop() {
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(7).joinToString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(3).drop(4).joinToString(limit = 10))
    }

    test fun take() {
        assertEquals("0, 1, 1, 2, 3, 5, 8", fibonacci().take(7).joinToString())
        assertEquals("2, 3, 5, 8", fibonacci().drop(3).take(4).joinToString())
    }

    test fun dropWhile() {
        assertEquals("233, 377, 610", fibonacci().dropWhile { it < 200 }.take(3).joinToString(limit = 10))
        assertEquals("", sequenceOf(1).dropWhile { it < 200 }.joinToString(limit = 10))
    }

    test fun merge() {
        expect(listOf("ab", "bc", "cd")) {
            sequenceOf("a", "b", "c").merge(sequenceOf("b", "c", "d")) { a, b -> a + b }.toList()
        }
    }

    test fun toStringJoinsNoMoreThanTheFirstTenElements() {
        assertEquals("0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...", fibonacci().joinToString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().filter { it > 10 }.joinToString(limit = 10))
        assertEquals("144, 233, 377, 610, 987", fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.joinToString())
    }

    test fun plus() {
        val sequence = sequenceOf("foo", "bar")
        val sequenceCheese = sequence + "cheese"
        assertEquals(listOf("foo", "bar", "cheese"), sequenceCheese.toList())

        // lets use a mutable variable
        var mi = sequenceOf("a", "b")
        mi += "c"
        assertEquals(listOf("a", "b", "c"), mi.toList())
    }

    test fun plusCollection() {
        val a = sequenceOf("foo", "bar")
        val b = listOf("cheese", "wine")
        val sequence = a + b
        assertEquals(listOf("foo", "bar", "cheese", "wine"), sequence.toList())

        // lets use a mutable variable
        var ml = sequenceOf("a")
        ml += a
        ml += "beer"
        ml += b
        ml += "z"
        assertEquals(listOf("a", "foo", "bar", "beer", "cheese", "wine", "z"), ml.toList())
    }


    test fun iterationOverSequence() {
        var s = ""
        for (i in sequenceOf(0, 1, 2, 3, 4, 5)) {
            s = s + i.toString()
        }
        assertEquals("012345", s)
    }

    test fun sequenceFromFunction() {
        var count = 3

        val sequence = sequence {
            count--
            if (count >= 0) count else null
        }

        val list = sequence.toList()
        assertEquals(listOf(2, 1, 0), list)

        fails {
            sequence.toList()
        }
    }

    test fun sequenceFromFunctionWithInitialValue() {
        val values = sequence(3) { n -> if (n > 0) n - 1 else null }
        val expected = listOf(3, 2, 1, 0)
        assertEquals(expected, values.toList())
        assertEquals(expected, values.toList(), "Iterating sequence second time yields the same result")
    }


    test fun sequenceFromIterator() {
        val list = listOf(3, 2, 1, 0)
        val iterator = list.iterator()
        val sequence = iterator.asSequence()
        assertEquals(list, sequence.toList())
        fails {
            sequence.toList()
        }
    }

    test fun makeSequenceOneTimeConstrained() {
        val sequence = sequenceOf(1, 2, 3, 4)
        sequence.toList()
        sequence.toList()

        val oneTime = sequence.constrainOnce()
        oneTime.toList()
        assertTrue("should fail with IllegalStateException") {
            fails {
                oneTime.toList()
            } is IllegalStateException
        }

    }

    private fun <T, C : MutableCollection<in T>> Sequence<T>.takeWhileTo(result: C, predicate: (T) -> Boolean): C {
        for (element in this) if (predicate(element)) result.add(element) else break
        return result
    }

    test fun sequenceExtensions() {
        val d = ArrayList<Int>()
        sequenceOf(0, 1, 2, 3, 4, 5).takeWhileTo(d, { i -> i < 4 })
        assertEquals(4, d.size())
    }

    test fun flatMapAndTakeExtractTheTransformedElements() {
        val expected = listOf(
                '3', // fibonacci(4) = 3
                '5', // fibonacci(5) = 5
                '8', // fibonacci(6) = 8
                '1', '3', // fibonacci(7) = 13
                '2', '1', // fibonacci(8) = 21
                '3', '4', // fibonacci(9) = 34
                '5' // fibonacci(10) = 55
                             )

        assertEquals(expected, fibonacci().drop(4).flatMap { it.toString().asSequence() }.take(10).toList())
    }

    test fun flatMap() {
        val result = sequenceOf(1, 2).flatMap { sequenceOf(0..it) }
        assertEquals(listOf(0, 1, 0, 1, 2), result.toList())
    }

    test fun flatMapOnEmpty() {
        val result = sequenceOf<Int>().flatMap { sequenceOf(0..it) }
        assertTrue(result.none())
    }

    test fun flatMapWithEmptyItems() {
        val result = sequenceOf(1, 2, 4).flatMap { if (it == 2) sequenceOf<Int>() else sequenceOf(it - 1..it) }
        assertEquals(listOf(0, 1, 3, 4), result.toList())
    }

    test
    fun flatten() {
        val data = sequenceOf(1, 2).map { sequenceOf(0..it) }
        assertEquals(listOf(0, 1, 0, 1, 2), data.flatten().toList())
    }

    test fun distinct() {
        val sequence = fibonacci().dropWhile { it < 10 }.take(20)
        assertEquals(listOf(1, 2, 3, 0), sequence.map { it % 4 }.distinct().toList())
    }

    test fun distinctBy() {
        val sequence = fibonacci().dropWhile { it < 10 }.take(20)
        assertEquals(listOf(13, 34, 55, 144), sequence.distinctBy { it % 4 }.toList())
    }


    /*
    test fun pairIterator() {
        val pairStr = (fibonacci() zip fibonacci().map { i -> i*2 }).joinToString(limit = 10)
        assertEquals("(0, 0), (1, 2), (1, 2), (2, 4), (3, 6), (5, 10), (8, 16), (13, 26), (21, 42), (34, 68), ...", pairStr)
    }
*/

}
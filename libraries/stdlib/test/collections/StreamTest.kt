package test.collections

import org.junit.Test
import kotlin.test.*
import java.util.*

fun fibonacci(): Stream<Int> {
    // fibonacci terms
    var index = 0;
    var a = 0;
    var b = 1
    return stream<Int> {
        when (index++) { 0 -> a; 1 -> b; else -> {
            val result = a + b; a = b; b = result; result
        } }
    }
}

public class StreamTest {

    Test fun requireNoNulls() {
        val stream = arrayListOf<String?>("foo", "bar").stream()
        val notNull = stream.requireNoNulls()
        assertEquals(arrayListOf("foo", "bar"), notNull.toList())

        val streamWithNulls = arrayListOf("foo", null, "bar").stream()
        val notNull2 = streamWithNulls.requireNoNulls() // shouldn't fail yet
        fails {
            // should throw an exception as we have a null
            notNull2.toList()
        }
    }

    test fun mapNotNull() {
        val data = arrayListOf(null, "foo", null, "bar").stream()
        val foo = data.mapNotNull { it.length }
        assertEquals(arrayListOf(3, 3), foo.toList())

        assertTrue {
            foo is Stream<Int>
        }
    }

    Test fun filterAndTakeWhileExtractTheElementsWithinRange() {
        assertEquals(arrayListOf(144, 233, 377, 610, 987), fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.toList())
    }

    Test fun foldReducesTheFirstNElements() {
        val sum = {(a: Int, b: Int) -> a + b }
        assertEquals(arrayListOf(13, 21, 34, 55, 89).fold(0, sum), fibonacci().filter { it > 10 }.take(5).fold(0, sum))
    }

    Test fun takeExtractsTheFirstNElements() {
        assertEquals(arrayListOf(0, 1, 1, 2, 3, 5, 8, 13, 21, 34), fibonacci().take(10).toList())
    }

    Test fun mapAndTakeWhileExtractTheTransformedElements() {
        assertEquals(arrayListOf(0, 3, 3, 6, 9, 15), fibonacci().map { it * 3 }.takeWhile {(i: Int) -> i < 20 }.toList())
    }

    Test fun joinConcatenatesTheFirstNElementsAboveAThreshold() {
        assertEquals("13, 21, 34, 55, 89, ...", fibonacci().filter { it > 10 }.makeString(separator = ", ", limit = 5))
    }

    Test fun skippingIterator() {
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(7).makeString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(3).drop(4).makeString(limit = 10))
    }

    Test fun toStringJoinsNoMoreThanTheFirstTenElements() {
        assertEquals("0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...", fibonacci().makeString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().filter { it > 10 }.makeString(limit = 10))
        assertEquals("144, 233, 377, 610, 987", fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.makeString())
    }

    Test fun plus() {
        val stream = listOf("foo", "bar").stream()
        val streamChease = stream + "cheese"
        assertEquals(listOf("foo", "bar", "cheese"), streamChease.toList())

        // lets use a mutable variable
        var mi = listOf("a", "b").stream()
        mi += "c"
        assertEquals(listOf("a", "b", "c"), mi.toList())
    }

    Test fun plusCollection() {
        val a = listOf("foo", "bar")
        val b = listOf("cheese", "wine")
        val stream = a.stream() + b
        assertEquals(listOf("foo", "bar", "cheese", "wine"), stream.toList())

        // lets use a mutable variable
        var ml = listOf("a").stream()
        ml += a
        ml += "beer"
        ml += b
        ml += "z"
        assertEquals(listOf("a", "foo", "bar", "beer", "cheese", "wine", "z"), ml.toList())
    }


    Test fun iterationOverStream() {
        val c = arrayListOf(0, 1, 2, 3, 4, 5)
        var s = ""
        for (i in c.stream()) {
            s = s + i.toString()
        }
        assertEquals("012345", s)
    }

    Test fun streamFromFunction() {
        var count = 3

        val stream = stream<Int> {
            count--
            if (count >= 0) count else null
        }

        val list = stream.toList()
        assertEquals(listOf(2, 1, 0), list)
    }

    Test fun streamFromFunctionWithInitialValue() {
        val values = stream<Int>(3) { n -> if (n > 0) n - 1 else null }
        assertEquals(arrayListOf(3, 2, 1, 0), values.toList())
    }

    private fun <T, C : MutableCollection<in T>> Stream<T>.takeWhileTo(result: C, predicate: (T) -> Boolean): C {
        for (element in this) if (predicate(element)) result.add(element) else break
        return result
    }

    Test fun streamExtensions() {
        val c = arrayListOf(0, 1, 2, 3, 4, 5)
        val d = ArrayList<Int>()
        c.stream().takeWhileTo(d, { i -> i < 4 })
        assertEquals(4, d.size())
    }

    /*
    Test fun pairIterator() {
        val pairStr = (fibonacci() zip fibonacci().map { i -> i*2 }).makeString(limit = 10)
        assertEquals("(0, 0), (1, 2), (1, 2), (2, 4), (3, 6), (5, 10), (8, 16), (13, 26), (21, 42), (34, 68), ...", pairStr)
    }
*/

}
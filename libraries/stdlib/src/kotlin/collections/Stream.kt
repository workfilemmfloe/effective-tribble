package kotlin

import kotlin.support.AbstractIterator

public trait Stream<out T> {
    public fun iterator(): Iterator<T>
}

public fun <T> streamOf(vararg elements : T) : Stream<T> = elements.stream()

public class FilteringStream<T>(val stream: Stream<T>, val sendWhen: Boolean = true, val predicate: (T) -> Boolean) : Stream<T> {
    override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {
        val iterator = stream.iterator()
        override fun computeNext() {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (predicate(item) == sendWhen) {
                    setNext(item)
                    return
                }
            }
            done()
        }
    }
}

public class TransformingStream<T, R>(val stream: Stream<T>, val transformer: (T) -> R) : Stream<R> {
    override fun iterator(): Iterator<R> = object : AbstractIterator<R>() {
        val iterator = stream.iterator()
        override fun computeNext() {
            if (iterator.hasNext()) {
                setNext(transformer(iterator.next()))
            } else {
                done()
            }
        }
    }
}

class ZippingStream<T1, T2>(val stream1: Stream<T1>, val stream2: Stream<T2>) : Stream<Pair<T1,T2>> {
    override fun iterator(): Iterator<Pair<T1,T2>> = object : AbstractIterator<Pair<T1,T2>>() {
        val iterator1 = stream1.iterator()
        val iterator2 = stream2.iterator()
        override fun computeNext() {
            if (iterator1.hasNext() && iterator2.hasNext()) {
                setNext(iterator1.next() to iterator2.next())
            } else {
                done()
            }
        }
    }
}

public class FlatteningStream<T, R>(val stream: Stream<T>, val transformer: (T) -> Stream<R>) : Stream<R> {
    override fun iterator(): Iterator<R> = object : AbstractIterator<R>() {
        val iterator = stream.iterator()
        var itemIterator: Iterator<R>? = null
        override fun computeNext() {
            while (itemIterator == null) {
                if (!iterator.hasNext()) {
                    done()
                    break;
                } else {
                    val element = iterator.next()
                    val nextItemIterator = transformer(element).iterator()
                    if (nextItemIterator.hasNext())
                        itemIterator = nextItemIterator
                }
            }

            val currentItemIterator = itemIterator
            if (currentItemIterator == null) {
                done()
            } else {
                setNext(currentItemIterator.next())
                if (!currentItemIterator.hasNext())
                    itemIterator = null
            }
        }
    }
}

public class Multistream<T>(val streams: Stream<Stream<T>>) : Stream<T> {
    override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {
        val iterator = streams.iterator()
        var streamIterator: Iterator<T>? = null
        override fun computeNext() {
            while (streamIterator == null) {
                if (!iterator.hasNext()) {
                    done()
                    break;
                } else {
                    val stream = iterator.next()
                    val nextStreamIterator = stream.iterator()
                    if (nextStreamIterator.hasNext())
                        streamIterator = nextStreamIterator
                }
            }

            val currentStreamIterator = streamIterator
            if (currentStreamIterator == null) {
                done()
            } else {
                setNext(currentStreamIterator.next())
                if (!currentStreamIterator.hasNext())
                    streamIterator = null
            }
        }
    }
}

public class LimitedStream<T>(val stream: Stream<T>, val stopWhen: Boolean = true, val predicate: (T) -> Boolean) : Stream<T> {
    override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {
        val iterator = stream.iterator()
        override fun computeNext() {
            if (!iterator.hasNext()) {
                done()
            } else {
                val item = iterator.next()
                if (predicate(item) == stopWhen) {
                    done()
                } else {
                    setNext(item)
                }
            }
        }
    }
}

public class FunctionStream<T : Any>(val producer: () -> T?) : Stream<T> {
    override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {

        override fun computeNext() {
            val item = producer()
            if (item == null) {
                done()
            } else {
                setNext(item)
            }
        }
    }
}

/**
 * Returns a stream which invokes the function to calculate the next value on each iteration until the function returns *null*
 */
public fun <T : Any> stream(nextFunction: () -> T?): Stream<T> {
    return FunctionStream(nextFunction)
}

/**
 * Returns a stream which invokes the function to calculate the next value based on the previous one on each iteration
 * until the function returns *null*
 */
public /*inline*/ fun <T : Any> stream(initialValue: T, nextFunction: (T) -> T?): Stream<T> =
        stream(nextFunction.toGenerator(initialValue))


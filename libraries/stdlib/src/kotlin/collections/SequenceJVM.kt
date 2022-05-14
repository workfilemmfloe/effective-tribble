@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SequencesKt")

package kotlin

import java.util.concurrent.atomic.AtomicReference


internal class ConstrainedOnceSequence<T>(sequence: Sequence<T>) : Sequence<T> {
    private val sequenceRef = AtomicReference(sequence)

    override fun iterator(): Iterator<T> {
        val sequence = sequenceRef.getAndSet(null) ?: throw IllegalStateException("This sequence can be consumed only once.")
        return sequence.iterator()
    }
}


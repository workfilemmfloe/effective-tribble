/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

@Deprecated("Use IntRange instead.", ReplaceWith("IntRange"), level = DeprecationLevel.WARNING)
/**
 * A range of values of type `Byte`.
 */
public class ByteRange(start: Byte, endInclusive: Byte) : ByteProgression(start, endInclusive, 1), ClosedRange<Byte> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Byte get() = endInclusive

    override val start: Byte get() = first
    override val endInclusive: Byte get() = last

    override fun contains(value: Byte): Boolean = start <= value && value <= endInclusive

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is ByteRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + endInclusive.toInt())

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Byte. */
        public val EMPTY: ByteRange = ByteRange(1, 0)
    }
}

/**
 * A range of values of type `Char`.
 */
public class CharRange(start: Char, endInclusive: Char) : CharProgression(start, endInclusive, 1), ClosedRange<Char> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Char get() = endInclusive

    override val start: Char get() = first
    override val endInclusive: Char get() = last

    override fun contains(value: Char): Boolean = start <= value && value <= endInclusive

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is CharRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + endInclusive.toInt())

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Char. */
        public val EMPTY: CharRange = CharRange(1.toChar(), 0.toChar())
    }
}

@Deprecated("Use IntRange instead.", ReplaceWith("IntRange"), level = DeprecationLevel.WARNING)
/**
 * A range of values of type `Short`.
 */
public class ShortRange(start: Short, endInclusive: Short) : ShortProgression(start, endInclusive, 1), ClosedRange<Short> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Short get() = endInclusive

    override val start: Short get() = first
    override val endInclusive: Short get() = last

    override fun contains(value: Short): Boolean = start <= value && value <= endInclusive

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is ShortRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + endInclusive.toInt())

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Short. */
        public val EMPTY: ShortRange = ShortRange(1, 0)
    }
}

/**
 * A range of values of type `Int`.
 */
public class IntRange(start: Int, endInclusive: Int) : IntProgression(start, endInclusive, 1), ClosedRange<Int> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Int get() = endInclusive

    override val start: Int get() = first
    override val endInclusive: Int get() = last

    override fun contains(value: Int): Boolean = start <= value && value <= endInclusive

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is IntRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start + endInclusive)

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Int. */
        public val EMPTY: IntRange = IntRange(1, 0)
    }
}

/**
 * A range of values of type `Long`.
 */
public class LongRange(start: Long, endInclusive: Long) : LongProgression(start, endInclusive, 1), ClosedRange<Long> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Long get() = endInclusive

    override val start: Long get() = first
    override val endInclusive: Long get() = last

    override fun contains(value: Long): Boolean = start <= value && value <= endInclusive

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is LongRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (start xor (start ushr 32)) + (endInclusive xor (endInclusive ushr 32))).toInt()

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Long. */
        public val EMPTY: LongRange = LongRange(1, 0)
    }
}


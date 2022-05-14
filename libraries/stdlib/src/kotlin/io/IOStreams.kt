package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder

/** Returns an [Iterator] of bytes in this input stream. */
public fun InputStream.iterator(): ByteIterator =
        object: ByteIterator() {
            override fun hasNext(): Boolean = available() > 0

            public override fun nextByte(): Byte = read().toByte()
        }

/**
 * Creates a buffered input stream wrapping this stream.
 * @param bufferSize the buffer size to use.
 */
public fun InputStream.buffered(bufferSize: Int = defaultBufferSize): InputStream
        = if (this is BufferedInputStream)
    this
else
    BufferedInputStream(this, bufferSize)

/** Creates a reader on this input stream using UTF-8 or the specified [charset]. */
public fun InputStream.reader(charset: Charset = Charsets.UTF_8): InputStreamReader = InputStreamReader(this, charset)

/** Creates a reader on this input stream using the specified [charset]. */
public fun InputStream.reader(charset: String): InputStreamReader = InputStreamReader(this, charset)

/** Creates a reader on this input stream using the specified [decoder]. */
public fun InputStream.reader(decoder: CharsetDecoder): InputStreamReader = InputStreamReader(this, decoder)

/**
 * Creates a buffered output stream wrapping this stream.
 * @param bufferSize the buffer size to use.
 */
public fun OutputStream.buffered(bufferSize: Int = defaultBufferSize): BufferedOutputStream
        = if (this is BufferedOutputStream) this else BufferedOutputStream(this, bufferSize)

/** Creates a writer on this output stream using UTF-8 or the specified [charset]. */
public fun OutputStream.writer(charset: Charset = Charsets.UTF_8): OutputStreamWriter = OutputStreamWriter(this, charset)

/** Creates a writer on this output stream using the specified [charset]. */
public fun OutputStream.writer(charset: String): OutputStreamWriter = OutputStreamWriter(this, charset)

/** Creates a writer on this output stream using the specified [encoder]. */
public fun OutputStream.writer(encoder: CharsetEncoder): OutputStreamWriter = OutputStreamWriter(this, encoder)

/**
 * Copies this stream to the given output stream, returning the number of bytes copied
 *
 * **Note** It is the caller's responsibility to close both of these resources.
 */
public fun InputStream.copyTo(out: OutputStream, bufferSize: Int = defaultBufferSize): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}

/**
 * Reads this stream completely into a byte array.
 *
 * **Note**: It is the caller's responsibility to close this stream.
 */
public fun InputStream.readBytes(estimatedSize: Int = defaultBufferSize): ByteArray {
    val buffer = ByteArrayOutputStream(estimatedSize)
    this.copyTo(buffer)
    return buffer.toByteArray()
}

import java.io.*

public class C {
    Throws(IOException::class)
    fun foo() {
        try {
            ByteArrayInputStream(ByteArray(10)).use { stream ->
                // reading something
                val c = stream.read()
                println(c)
            }
        } finally {
            // dispose something else
        }
    }
}
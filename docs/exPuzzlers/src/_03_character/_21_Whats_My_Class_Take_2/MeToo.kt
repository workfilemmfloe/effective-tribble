namespace whats.my.`class`.take2

import kotlin.io.*
import kotlin.string.*
import kotlin.jutils.*
import java.io.File;

class MeToo() {
    fun main() {
        println(getJavaClass().getCanonicalName()?.replaceAllSubstrings(".", /*File.separator.sure()*/"\\"))

        // Regex is denoted explicitly
        println(getJavaClass().getCanonicalName()?.replaceAllWithRegex(".", "\\"))
    }
}

fun main(args : Array<String>) {
    MeToo().main()
}

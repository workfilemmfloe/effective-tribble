namespace line.printer

import kotlin.io.*
import kotlin.*

fun main(args : Array<String>) {
    // Note: \u000A is Unicode representation of linefeed (LF)
    val c : Char = 0x000A .toChar();
    println(c);
}

fun testInt () : String {
    val r1 = 1 rangeTo 4
    if(r1.end != 4 || r1.isReversed || r1.size != 4) return "int rangeTo fail"

    val r2 = 4 rangeTo 1
    if(r2.start != 0 || r2.size != 0) return "int negative rangeTo fail"

    val r3 = 5 downTo 0
    if(r3.start != 5 || r3.end != 0 || !r3.isReversed || r3.size != 6) return "int downTo fail"

    val r4 = 5 downTo 6
    if(r4.start != 0 || r4.end != 0 || !r3.isReversed || r4.size != 0) return "int negative downTo fail"

    return "OK"
}

fun testByte () : String {
    val r1 = 1.toByte() rangeTo 4.toByte()
    if(r1.end != 4.toByte() || r1.isReversed || r1.size != 4) return "byte rangeTo fail"

    val r2 = 4.toByte() rangeTo 1.toByte()
    if(r2.start != 0.toByte() || r2.size != 0) return "byte negative rangeTo fail"

    val r3 = 5.toByte() downTo 0.toByte()
    if(r3.start != 5.toByte() || r3.end != 0.toByte() || !r3.isReversed || r3.size != 6) return "byte downTo fail"

    val r4 = 5.toByte() downTo 6.toByte()
    if(r4.start != 0.toByte() || r4.end != 0.toByte() || !r3.isReversed || r4.size != 0) return "byte negative downTo fail"

    return "OK"
}

fun testShort () : String {

    val r1 = 1.toShort() rangeTo 4.toShort()
    if(r1.end != 4.toShort() || r1.isReversed || r1.size != 4) return "short rangeTo fail"

    val r2 = 4.toShort() rangeTo 1.toShort()
    if(r2.start != 0.toShort() || r2.size != 0) return "short negative rangeTo fail"

    val r3 = 5.toShort() downTo 0.toShort()
    if(r3.start != 5.toShort() || r3.end != 0.toShort() || !r3.isReversed || r3.size != 6) return "short downTo fail"

    val r4 = 5.toShort() downTo 6.toShort()
    if(r4.start != 0.toShort() || r4.end != 0.toShort() || !r3.isReversed || r4.size != 0) return "short negative downTo fail"

    return "OK"
}

fun testLong () : String {

    val r1 = 1.toLong() rangeTo 4.toLong()
    if(r1.end != 4.toLong() || r1.isReversed || r1.size != 4.toLong()) return "long rangeTo fail"

    val r2 = 4.toLong() rangeTo 1.toLong()
    if(r2.start != 0.toLong() || r2.size != 0.toLong()) return "short negative long fail"

    val r3 = 5.toLong() downTo 0.toLong()
    if(r3.start != 5.toLong() || r3.end != 0.toLong() || !r3.isReversed || r3.size != 6.toLong()) return "long downTo fail"

    val r4 = 5.toLong() downTo 6.toLong()
    if(r4.start != 0.toLong() || r4.end != 0.toLong() || !r3.isReversed || r4.size != 0.toLong()) return "long negative downTo fail"

    return "OK"
}

fun testChar () : String {

    val r1 = 'a' rangeTo 'd'
    if(r1.end != 'd' || r1.isReversed || r1.size != 4) return "char rangeTo fail"

    val r2 = 'd' rangeTo 'a'
    if(r2.start != 0.toChar() || r2.size != 0) return "char negative long fail"

    val r3 = 'd' downTo 'a'
    if(r3.start != 'd' || r3.end != 'a' || !r3.isReversed || r3.size != 4) return "char downTo fail"

    val r4 = 'a' downTo 'd'
    if(r4.start != 0.toChar() || r4.end != 0.toChar() || !r3.isReversed || r4.size != 0) return "char negative downTo fail"

    return "OK"
}

fun box() : String {
    var r : String

    r = testInt()
    if(r != "OK") return r

    r = testByte()
    if(r != "OK") return r

    r = testShort()
    if(r != "OK") return r

    r = testLong()
    if(r != "OK") return r

   return "OK"
}
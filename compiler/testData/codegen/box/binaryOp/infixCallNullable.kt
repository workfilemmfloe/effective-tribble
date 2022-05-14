fun box(): String {
    val a1: Byte? = 1 plus 1
    val a2: Short? = 1 plus 1
    val a3: Int? = 1 plus 1
    val a4: Long? = 1 plus 1
    val a5: Double? = 1.0 plus 1
    val a6: Float? = 1f plus 1

    if (a1!! != 2.toByte()) return "fail 1"
    if (a2!! != 2.toShort()) return "fail 2"
    if (a3!! != 2) return "fail 3"
    if (a4!! != 2L) return "fail 4"
    if (a5!! != 2.0) return "fail 5"
    if (a6!! != 2f) return "fail 6"

    return "OK"
}

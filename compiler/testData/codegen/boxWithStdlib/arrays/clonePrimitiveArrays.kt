import java.util.Arrays.equals

fun box(): String {
    val i = intArrayOf(1, 2)
    if (!equals(i, i.clone())) return "Fail int"
    if (i.clone().identityEquals(i)) return "Fail int identity"

    val j = longArrayOf(1L, 2L)
    if (!equals(j, j.clone())) return "Fail long"
    if (j.clone().identityEquals(j)) return "Fail long identity"

    val s = shortArrayOf(1.toShort(), 2.toShort())
    if (!equals(s, s.clone())) return "Fail short"
    if (s.clone().identityEquals(s)) return "Fail short identity"

    val b = byteArrayOf(1.toByte(), 2.toByte())
    if (!equals(b, b.clone())) return "Fail byte"
    if (b.clone().identityEquals(b)) return "Fail byte identity"

    val c = charArrayOf('a', 'b')
    if (!equals(c, c.clone())) return "Fail char"
    if (c.clone().identityEquals(c)) return "Fail char identity"

    val d = doubleArrayOf(1.0, -1.0)
    if (!equals(d, d.clone())) return "Fail double"
    if (d.clone().identityEquals(d)) return "Fail double identity"

    val f = floatArrayOf(1f, -1f)
    if (!equals(f, f.clone())) return "Fail float"
    if (f.clone().identityEquals(f)) return "Fail float identity"

    val z = booleanArrayOf(true, false)
    if (!equals(z, z.clone())) return "Fail boolean"
    if (z.clone().identityEquals(z)) return "Fail boolean identity"

    return "OK"
}

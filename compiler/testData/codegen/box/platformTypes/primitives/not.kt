fun box(): String {
    val l = java.util.ArrayList<Boolean>()
    l.add(true)
    val x = !l[0]
    if (x) return "Fail: $x}"
    return "OK"
}
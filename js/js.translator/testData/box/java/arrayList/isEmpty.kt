package foo

import java.util.ArrayList;

fun box(): String {
    val a = ArrayList<Int>();
    a.add(3)
    if (a.isEmpty()) return "fail1"
    if (!ArrayList<Int>().isEmpty()) return "fail2"
    return "OK"
}
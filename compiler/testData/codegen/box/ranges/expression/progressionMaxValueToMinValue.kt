// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// TODO: muted automatically, investigate should it be ran for NATIVE or not
// IGNORE_BACKEND: NATIVE

// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
// WITH_RUNTIME


import java.lang.Integer.MAX_VALUE as MaxI
import java.lang.Integer.MIN_VALUE as MinI
import java.lang.Byte.MAX_VALUE as MaxB
import java.lang.Byte.MIN_VALUE as MinB
import java.lang.Short.MAX_VALUE as MaxS
import java.lang.Short.MIN_VALUE as MinS
import java.lang.Long.MAX_VALUE as MaxL
import java.lang.Long.MIN_VALUE as MinL
import java.lang.Character.MAX_VALUE as MaxC
import java.lang.Character.MIN_VALUE as MinC

fun box(): String {
    val list1 = ArrayList<Int>()
    val range1 = MaxI..MinI step 1
    for (i in range1) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<Int>()) {
        return "Wrong elements for MaxI..MinI step 1: $list1"
    }

    val list2 = ArrayList<Int>()
    val range2 = MaxB..MinB step 1
    for (i in range2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<Int>()) {
        return "Wrong elements for MaxB..MinB step 1: $list2"
    }

    val list3 = ArrayList<Int>()
    val range3 = MaxS..MinS step 1
    for (i in range3) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<Int>()) {
        return "Wrong elements for MaxS..MinS step 1: $list3"
    }

    val list4 = ArrayList<Long>()
    val range4 = MaxL..MinL step 1
    for (i in range4) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<Long>()) {
        return "Wrong elements for MaxL..MinL step 1: $list4"
    }

    val list5 = ArrayList<Char>()
    val range5 = MaxC..MinC step 1
    for (i in range5) {
        list5.add(i)
        if (list5.size > 23) break
    }
    if (list5 != listOf<Char>()) {
        return "Wrong elements for MaxC..MinC step 1: $list5"
    }

    return "OK"
}

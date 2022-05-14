// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
// WITH_RUNTIME


fun box(): String {
    val list1 = ArrayList<Int>()
    val range1 = (8 downTo 3 step 2).reversed()
    for (i in range1) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<Int>(4, 6, 8)) {
        return "Wrong elements for (8 downTo 3 step 2).reversed(): $list1"
    }

    val list2 = ArrayList<Int>()
    val range2 = (8.toByte() downTo 3.toByte() step 2).reversed()
    for (i in range2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<Int>(4, 6, 8)) {
        return "Wrong elements for (8.toByte() downTo 3.toByte() step 2).reversed(): $list2"
    }

    val list3 = ArrayList<Int>()
    val range3 = (8.toShort() downTo 3.toShort() step 2).reversed()
    for (i in range3) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<Int>(4, 6, 8)) {
        return "Wrong elements for (8.toShort() downTo 3.toShort() step 2).reversed(): $list3"
    }

    val list4 = ArrayList<Long>()
    val range4 = (8.toLong() downTo 3.toLong() step 2.toLong()).reversed()
    for (i in range4) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<Long>(4, 6, 8)) {
        return "Wrong elements for (8.toLong() downTo 3.toLong() step 2.toLong()).reversed(): $list4"
    }

    val list5 = ArrayList<Char>()
    val range5 = ('d' downTo 'a' step 2).reversed()
    for (i in range5) {
        list5.add(i)
        if (list5.size > 23) break
    }
    if (list5 != listOf<Char>('b', 'd')) {
        return "Wrong elements for ('d' downTo 'a' step 2).reversed(): $list5"
    }

    return "OK"
}

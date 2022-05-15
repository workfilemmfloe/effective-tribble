// TODO: muted automatically, investigate should it be ran for JS_IR or not
// IGNORE_BACKEND: JS_IR

// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
// WITH_RUNTIME



fun box(): String {
    val list1 = ArrayList<Int>()
    for (i in 3..9 step 2) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<Int>(3, 5, 7, 9)) {
        return "Wrong elements for 3..9 step 2: $list1"
    }

    val list2 = ArrayList<Int>()
    for (i in 3.toByte()..9.toByte() step 2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<Int>(3, 5, 7, 9)) {
        return "Wrong elements for 3.toByte()..9.toByte() step 2: $list2"
    }

    val list3 = ArrayList<Int>()
    for (i in 3.toShort()..9.toShort() step 2) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<Int>(3, 5, 7, 9)) {
        return "Wrong elements for 3.toShort()..9.toShort() step 2: $list3"
    }

    val list4 = ArrayList<Long>()
    for (i in 3L..9L step 2L) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<Long>(3, 5, 7, 9)) {
        return "Wrong elements for 3L..9L step 2L: $list4"
    }

    val list5 = ArrayList<Char>()
    for (i in 'c'..'g' step 2) {
        list5.add(i)
        if (list5.size > 23) break
    }
    if (list5 != listOf<Char>('c', 'e', 'g')) {
        return "Wrong elements for 'c'..'g' step 2: $list5"
    }

    val list6 = ArrayList<UInt>()
    for (i in 3u..9u step 2) {
        list6.add(i)
        if (list6.size > 23) break
    }
    if (list6 != listOf<UInt>(3u, 5u, 7u, 9u)) {
        return "Wrong elements for 3u..9u step 2: $list6"
    }

    val list7 = ArrayList<UInt>()
    for (i in 3u.toUByte()..9u.toUByte() step 2) {
        list7.add(i)
        if (list7.size > 23) break
    }
    if (list7 != listOf<UInt>(3u, 5u, 7u, 9u)) {
        return "Wrong elements for 3u.toUByte()..9u.toUByte() step 2: $list7"
    }

    val list8 = ArrayList<UInt>()
    for (i in 3u.toUShort()..9u.toUShort() step 2) {
        list8.add(i)
        if (list8.size > 23) break
    }
    if (list8 != listOf<UInt>(3u, 5u, 7u, 9u)) {
        return "Wrong elements for 3u.toUShort()..9u.toUShort() step 2: $list8"
    }

    val list9 = ArrayList<ULong>()
    for (i in 3uL..9uL step 2L) {
        list9.add(i)
        if (list9.size > 23) break
    }
    if (list9 != listOf<ULong>(3u, 5u, 7u, 9u)) {
        return "Wrong elements for 3uL..9uL step 2L: $list9"
    }

    return "OK"
}

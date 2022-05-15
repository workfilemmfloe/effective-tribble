// TODO: muted automatically, investigate should it be ran for JVM_IR or not
// IGNORE_BACKEND: JVM_IR

// TODO: muted automatically, investigate should it be ran for JS_IR or not
// IGNORE_BACKEND: JS_IR

// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
// WITH_RUNTIME


const val MaxI = Int.MAX_VALUE
const val MaxB = Byte.MAX_VALUE
const val MaxS = Short.MAX_VALUE
const val MaxL = Long.MAX_VALUE
const val MaxC = Char.MAX_VALUE
const val MaxUI = UInt.MAX_VALUE
const val MaxUB = UByte.MAX_VALUE
const val MaxUS = UShort.MAX_VALUE
const val MaxUL = ULong.MAX_VALUE

fun box(): String {
    val list1 = ArrayList<Int>()
    val range1 = (MaxI - 5)..MaxI step 3
    for (i in range1) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<Int>(MaxI - 5, MaxI - 2)) {
        return "Wrong elements for (MaxI - 5)..MaxI step 3: $list1"
    }

    val list2 = ArrayList<Int>()
    val range2 = (MaxB - 5).toByte()..MaxB step 3
    for (i in range2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<Int>((MaxB - 5).toInt(), (MaxB - 2).toInt())) {
        return "Wrong elements for (MaxB - 5).toByte()..MaxB step 3: $list2"
    }

    val list3 = ArrayList<Int>()
    val range3 = (MaxS - 5).toShort()..MaxS step 3
    for (i in range3) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<Int>((MaxS - 5).toInt(), (MaxS - 2).toInt())) {
        return "Wrong elements for (MaxS - 5).toShort()..MaxS step 3: $list3"
    }

    val list4 = ArrayList<Long>()
    val range4 = (MaxL - 5).toLong()..MaxL step 3
    for (i in range4) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<Long>((MaxL - 5).toLong(), (MaxL - 2).toLong())) {
        return "Wrong elements for (MaxL - 5).toLong()..MaxL step 3: $list4"
    }

    val list5 = ArrayList<Char>()
    val range5 = (MaxC - 5)..MaxC step 3
    for (i in range5) {
        list5.add(i)
        if (list5.size > 23) break
    }
    if (list5 != listOf<Char>((MaxC - 5), (MaxC - 2))) {
        return "Wrong elements for (MaxC - 5)..MaxC step 3: $list5"
    }

    val list6 = ArrayList<UInt>()
    val range6 = (MaxUI - 5u)..MaxUI step 3
    for (i in range6) {
        list6.add(i)
        if (list6.size > 23) break
    }
    if (list6 != listOf<UInt>(MaxUI - 5u, MaxUI - 2u)) {
        return "Wrong elements for (MaxUI - 5u)..MaxUI step 3: $list6"
    }

    val list7 = ArrayList<UInt>()
    val range7 = (MaxUB - 5u).toUByte()..MaxUB step 3
    for (i in range7) {
        list7.add(i)
        if (list7.size > 23) break
    }
    if (list7 != listOf<UInt>((MaxUB - 5u).toUInt(), (MaxUB - 2u).toUInt())) {
        return "Wrong elements for (MaxUB - 5u).toUByte()..MaxUB step 3: $list7"
    }

    val list8 = ArrayList<UInt>()
    val range8 = (MaxUS - 5u).toUShort()..MaxUS step 3
    for (i in range8) {
        list8.add(i)
        if (list8.size > 23) break
    }
    if (list8 != listOf<UInt>((MaxUS - 5u).toUInt(), (MaxUS - 2u).toUInt())) {
        return "Wrong elements for (MaxUS - 5u).toUShort()..MaxUS step 3: $list8"
    }

    val list9 = ArrayList<ULong>()
    val range9 = (MaxUL - 5u)..MaxUL step 3
    for (i in range9) {
        list9.add(i)
        if (list9.size > 23) break
    }
    if (list9 != listOf<ULong>((MaxUL - 5u), (MaxUL - 2u))) {
        return "Wrong elements for (MaxUL - 5u)..MaxUL step 3: $list9"
    }

    return "OK"
}

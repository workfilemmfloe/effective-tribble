// "Replace with 'add(c)'" "true"
class C

@Deprecated("", ReplaceWith("add(c)"))
operator fun C.plus(c: C) = C()

fun C.add(c: C) = C()

fun f() {
    var c1 = C()
    c1 <caret>+= C()
}
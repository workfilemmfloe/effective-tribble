class A
class B

fun A.ext() {
    val x = ::A
    val y = ::B

    x : KFunction0<A>
    y : KFunction0<B>
}

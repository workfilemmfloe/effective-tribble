class M<out V>
class X

val p: M<X> = throw Exception()

// method: namespace::getP
// jvm signature:     ()LM;
// generic signature: ()LM<LX;>;
// kotlin signature:  null

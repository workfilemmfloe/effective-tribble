class M<out V>
class X

val p: M<X> = throw Exception()

// method: _DefaultPackage::getP
// jvm signature:     ()LM;
// generic signature: ()LM<LX;>;
class In<in T>
class X

fun f(p: In<In<X>>) {}

// method: namespace::f
// jvm signature:     (LIn;)V
// generic signature: (LIn<-LIn<-LX;>;>;)V
// kotlin signature:  (LIn<LIn<LX;>;>;)V

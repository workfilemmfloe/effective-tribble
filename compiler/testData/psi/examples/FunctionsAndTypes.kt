type f1 =  (T) -> X
// type f1 = {(T) => X}
type f2 =  (T, E) -> X
// type f2 = {(T, E) => X}
type f_tuple =  (Pair<T, E>) -> X
//type f_tuple = {((T, E)) => X}
type hof =   (X) ->  (T) -> Y
//type hof = { (X) => {(T) => Y} }
type hof2 =   ( (X) -> Y) ->  (Y) -> Z
//type hof2 = { {(X) => Y} => {(Y) => Z} }


type Comparison<in T> =  (a : T, b : T) -> Int
//type Comparison<in T> = {(a : T, b : T) => Int}
type Equality<in T> =  (a : T, b : T) -> Boolean
//type Equality<in T> = {(a : T, b : T) => Boolean}
type HashFunction<in T> =  (obj : T) -> Int
//type HashFunction<in T> = {(obj : T) => Int}
type Runnable =  () -> Unit
//type Runnable = {() => ()}
type Function1<in T, out R> =  (input : T) -> R
//type Function1<in T, out R> = {(input : T) => R}


val f1 = {(t : T) : X -> something(t)}
fun f1(t : T) : X = something(t)

val f1 = {(t : T) -> something(t)}
val f1 = {(T) : X -> something(it)}
val f1 = {t -> something(t)}
val f1 = {something(it)}

val f1 :  (T) -> X = {X()}


fun <T> getT(): T = null!!

val foo = getT<List<in Int>>()
/*
psi: val foo = getT<List<in Int>>()
type: [Error type: Resolution error type (Inconsistent type: List<in Int> (0 parameter has declared variance: out, but argument variance is in))]
*/
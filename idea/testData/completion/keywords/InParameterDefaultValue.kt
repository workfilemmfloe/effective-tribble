fun foo(p: Int = <caret>)

// EXIST: do
// EXIST: false
// EXIST: for
// EXIST: if
// EXIST: null
// EXIST: object
// EXIST: package
// EXIST: return
// EXIST: super
// EXIST: throw
// EXIST: true
// EXIST: try
// EXIST: when
// EXIST: while
// NUMBER: 14

package test

class A

fun <T> some() where T: <caret>A {}

// REF: (test).A
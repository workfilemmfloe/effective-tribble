// MODULE: module1
// FILE: module1.kt

package foo

@native
@JsName("A")
open class B(val foo: String)

// MODULE: main(module1)
// FILE: main.kt
package foo

class C(s: String) : B(s)

fun box(): String {
    return C("OK").foo
}

// FILE: test.js

function A(foo) {
    this.foo = foo;
}
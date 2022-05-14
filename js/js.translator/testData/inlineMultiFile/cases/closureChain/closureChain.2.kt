/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/complex/closureChain.2.kt
 */

package foo

class Inline() {

    inline fun foo(closure1 : (l: Int) -> String, param: Int, closure2: String.() -> Int) : Int {
        return closure1(param).closure2()
    }
}


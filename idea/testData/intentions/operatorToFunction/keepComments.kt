interface C {
    operator fun set(p1: String, p2: String, value: Int)
}

class D(val c: C) {
    fun foo() {
        this.c/* and this is c */[<caret>
                "a", // it's "a"
                "b" /* and this is "b" */] = /* we use 10 */10
    }
}

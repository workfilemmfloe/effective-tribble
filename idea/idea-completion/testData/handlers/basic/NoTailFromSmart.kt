class CCC {
    companion object {
        fun create(): CCC = CCC()
    }
}

fun f(ccc: CCC, p: Int){}

fun g() {
    f(<caret>)
}

// ELEMENT: create

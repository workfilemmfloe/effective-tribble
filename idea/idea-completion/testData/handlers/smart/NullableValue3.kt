class K {
    companion object {
        fun bar(): K? = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT_TEXT: "!! K.bar"
// TAIL_TEXT: "() (<root>)"

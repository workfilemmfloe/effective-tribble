package test

public trait ChangeProjectionKind2 {

    public trait Super {
        public fun foo(p: MutableList<String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p: MutableList<String>)
    }
}

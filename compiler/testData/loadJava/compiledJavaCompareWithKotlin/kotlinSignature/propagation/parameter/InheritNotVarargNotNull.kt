package test

public trait InheritNotVarargNotNull: Object {

    public trait Super: Object {
        public fun foo(p0: Array<out String>)
    }

    public trait Sub: Super {
        override fun foo(p0: Array<out String>)
    }
}

package test

public trait InheritVararg: Object {

    public trait Super: Object {
        public fun foo(vararg p0: String?)
    }

    public trait Sub: Super {
        override fun foo(vararg p0: String?)
    }
}

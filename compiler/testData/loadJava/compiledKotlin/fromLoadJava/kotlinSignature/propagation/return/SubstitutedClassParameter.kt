package test

public trait SubstitutedClassParameter {

    public trait Super<T> {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super<String> {
        override fun foo(): String
    }
}

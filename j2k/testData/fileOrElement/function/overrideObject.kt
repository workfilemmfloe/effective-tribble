// ERROR: Unresolved reference: clone
internal class X {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        return super.toString()
    }

    @Throws(CloneNotSupportedException::class)
    protected fun clone(): Any {
        return super.clone()
    }
}

internal class Y : Thread() {
    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }
}
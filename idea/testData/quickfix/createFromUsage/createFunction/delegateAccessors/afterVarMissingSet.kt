// "Create function 'set' from usage" "true"
class F {
    fun get(x: X, propertyMetadata: PropertyMetadata): Int = 1

    fun set(x: X, propertyMetadata: PropertyMetadata, i: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class X {
    var f: Int by F()
}

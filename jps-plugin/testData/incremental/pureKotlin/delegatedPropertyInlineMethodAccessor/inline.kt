package inline

class Inline {
    inline fun getValue(receiver: Any?, prop: PropertyMetadata): Int {
        return 0
    }

    inline fun setValue(receiver: Any?, prop: PropertyMetadata, value: Int) {
        println(value)
    }
}
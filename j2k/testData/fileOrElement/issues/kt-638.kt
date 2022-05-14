// ERROR: Property must be initialized or be abstract
public class Identifier<T> {
    public val name: T
    private val myHasDollar: Boolean
    private var myNullable = true

    public constructor(name: T) {
        this.name = name
    }

    public constructor(name: T, isNullable: Boolean) {
        this.name = name
        myNullable = isNullable
    }

    public constructor(name: T, hasDollar: Boolean, isNullable: Boolean) {
        this.name = name
        myHasDollar = hasDollar
        myNullable = isNullable
    }
}

public class User {
    companion object {
        public fun main(args: Array<String>) {
            val i1 = Identifier("name", false, true)
            val i2 = Identifier("name", false)
            val i3 = Identifier("name")
        }
    }
}

fun main(args: Array<String>) = User.main(args)
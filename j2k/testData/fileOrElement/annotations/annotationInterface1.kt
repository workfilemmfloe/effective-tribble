import java.lang.annotation.ElementType
import java.lang.annotation.Target

annotation class Anon(public val stringArray: Array<String>, public val intArray: IntArray, // string
                      public val string: String)

Anon(string = "a", stringArray = arrayOf("a", "b"), intArray = intArrayOf(1, 2))
Target(ElementType.CONSTRUCTOR, ElementType.FIELD)
annotation class I

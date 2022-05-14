//KT-2851 Type inference failed passing in not-null after smart-cast value in Pair
package a

fun main(args: Array<String>) {
    val value: String? = ""
    if (value != null) {
        foo(Pair("val", <!DEBUG_INFO_AUTOCAST!>value<!>))
        foo(Pair("val", value<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>))
        foo(Pair<String, String>("val", <!DEBUG_INFO_AUTOCAST!>value<!>))
    }
}

fun foo(<!UNUSED_PARAMETER!>map<!>: Pair<String, String>) {}


//from library
public class Pair<out A, out B> (
        public val first: A,
        public val second: B
)
// WITH_RUNTIME
// PARAM_DESCRIPTOR: val data: (kotlin.MutableList<(kotlin.String..kotlin.String?)>..kotlin.List<(kotlin.String..kotlin.String?)>) defined in test
// PARAM_TYPES: kotlin.List<(kotlin.String..kotlin.String?)>, kotlin.MutableList<(kotlin.String..kotlin.String?)>, kotlin.MutableCollection<(kotlin.String..kotlin.String?)>, kotlin.Collection<(kotlin.String..kotlin.String?)>
fun test(): Boolean {
    val j: J? = null
    val data = j?.getData() ?: return false
    return <selection>data.contains("foo")</selection>
}
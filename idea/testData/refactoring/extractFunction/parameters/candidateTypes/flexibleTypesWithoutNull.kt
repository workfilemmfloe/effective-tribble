// WITH_RUNTIME
// PARAM_TYPES: String, CharSequence
// PARAM_DESCRIPTOR: val property: (String..String?) defined in test
fun test() {
    val property = System.getProperty("some")
    val n = <selection>property.length()</selection>
}
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATED_ANNOTATION_SYNTAX
annotation class Ann

fun <T> bar(block: (T) -> Int) {}

fun foo() {
    bar<Int> @Ann [Ann] { x -> x }
    bar<Int> @Ann [Ann] label@{ x -> x }
}

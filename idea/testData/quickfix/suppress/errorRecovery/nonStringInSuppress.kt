// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"
// ERROR: An integer literal does not conform to the expected type kotlin.String

@Suppress(1)
fun foo(): String?<caret>? = null
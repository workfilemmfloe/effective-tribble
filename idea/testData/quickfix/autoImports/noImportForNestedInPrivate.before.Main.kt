// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Create local variable 'Nested'
// ACTION: Create object 'Nested'
// ACTION: Create parameter 'Nested'
// ACTION: Create property 'Nested'
// ERROR: Unresolved reference: Nested

fun test() {
    <caret>Nested
}
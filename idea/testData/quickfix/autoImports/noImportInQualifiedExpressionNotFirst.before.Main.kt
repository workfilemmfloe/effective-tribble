// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Create class 'SomeTest'
// ACTION: Rename reference
// ERROR: Unresolved reference: SomeTest

package testing

val x = testing.<caret>SomeTest()

// "Create object 'A'" "false"
// ACTION: Create extension property 'A'
// ACTION: Create member property 'A'
// ACTION: Create property 'A' as constructor parameter
// ACTION: Rename reference
// ERROR: Unresolved reference: A
package p

fun foo() = X().<caret>A

class X {

}
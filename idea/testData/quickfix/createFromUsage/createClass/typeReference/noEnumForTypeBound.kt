// "Create enum 'NotExistent'" "false"
// ACTION: Create class 'NotExistent'
// ACTION: Create interface 'NotExistent'
// ACTION: Create test
// ERROR: Unresolved reference: NotExistent
class TPB<X : <caret>NotExistent>
// "Insert 'this()' call" "true"
// ERROR: None of the following functions can be called with the arguments supplied: <br>public constructor A(x: kotlin.Int) defined in A<br>public constructor A(x: kotlin.String) defined in A

class A(val x: Int) {
    constructor(x: String)<caret>
}

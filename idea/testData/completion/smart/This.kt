fun String.foo(){
    val s : String = <caret>
}

// EXIST: { lookupString:"this", typeText:"String" }

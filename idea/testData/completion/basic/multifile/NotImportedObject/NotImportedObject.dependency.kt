package test

object NamedObjectTopLevel1
object NamedObjectTopLevel2

fun some() {
  object NamedObjectInFun
}

class Some {
  class object {
    object NamedObjectInClassObject
  }
}

// ALLOW_AST_ACCESS
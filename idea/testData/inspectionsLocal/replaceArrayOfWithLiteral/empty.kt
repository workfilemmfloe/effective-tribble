// LANGUAGE_VERSION: 1.2

annotation class Some(val strings: Array<String>)

@Some(strings = <caret>emptyArray())
class My

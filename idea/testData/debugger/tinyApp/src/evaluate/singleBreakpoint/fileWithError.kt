package fileWithError

fun main(args: Array<String>) {
    // There is an error about internal visibility while analyzing fileWithInternal.kt
    fileWithInternal.test()
}

// ADDITIONAL_BREAKPOINT: fileWithInternal.kt:Breakpoint

// EXPRESSION: 1
// RESULT: 1: I
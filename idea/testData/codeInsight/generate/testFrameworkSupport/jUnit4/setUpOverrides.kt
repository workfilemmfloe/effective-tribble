// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateTestSupportActionBase$SetUp
// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
open class A {
    open fun setUp() {

    }
}

class B : A() {<caret>

}
import java.io.File

fun test() {
  val f = File("testFile")
  // Should be no ;
  f.deleteOnExit<caret>
}

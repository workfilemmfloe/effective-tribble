import java.util.Date

data class Test(
  @Deprecated("Other")
  val some: Int? = 1,
  val other: Date = Date<caret>,
  val test: Int
)

// ELEMENT: Date
// TAIL_TEXT: "(...) (java.util)"

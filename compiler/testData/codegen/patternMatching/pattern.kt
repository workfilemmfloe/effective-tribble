fun isString(x: Any) = when(x) {
  is String -> "string"
  else -> "something"
}

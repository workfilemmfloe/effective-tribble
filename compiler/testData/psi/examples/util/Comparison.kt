typealias Comparison<in T> =  (T, T) -> Int

fun naturalOrder<in T : Comparable<T>>(a : T, b : T) : Int = a.compareTo(b)

fun castingNaturalOrder(a : Object, b : Object) : Int = (a as Comparable<Object>).compareTo(b as Comparable<Object>)

enum class ComparisonResult {
  LS, EQ, GR;
}

typealias MatchableComparison<in T> =  (T, T) -> ComparisonResult

fun asMatchableComparison<T>(cmp : Comparison<T>) : MatchableComparison<T> = {(a, b) ->
  val res = cmp(a, b)
  if (res == 0) return ComparisonResult.EQ
  if (res < 0) return ComparisonResult.LS
  return ComparisonResult.GR
}


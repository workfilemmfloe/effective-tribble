class Reluctant() {
     {
        throw Exception("I'm not coming out")
     }
}

fun p(o : Any?) = System.out?.println(o)

fun test1() : String {
  try {
      val b = Reluctant()
      return "Surprise!"
  }
  catch (ex : Exception) {
      return "I told you so"
  }
}


fun box() : String {
    if(test1() != "I told you so") return "test1 failed"

    return "OK"
}

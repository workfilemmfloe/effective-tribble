class Controller {
    var wasHandleResultCalled = false
    suspend fun suspendHere(x: Continuation<String>) {
        x.resume("OK")
    }

    operator fun handleResult(x: Unit, y: Continuation<Nothing>) {
        wasHandleResultCalled = true
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    val controller = Controller()
    c(controller).resume(Unit)

    if (!controller.wasHandleResultCalled) throw java.lang.RuntimeException("fail 1")
}

var varWithCustomSetter: String = ""
    set(value) {
        if (field != "") throw java.lang.RuntimeException("fail 2")
        field = value
    }

fun box(): String {
    var result = ""

    builder {
        result += "O"

        if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 3")

        result += "K"
    }

    if (result != "OK") return "fail 4"

    builder {
        if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 5")

        varWithCustomSetter = "OK"
    }

    return varWithCustomSetter
}

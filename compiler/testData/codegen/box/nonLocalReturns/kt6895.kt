// TARGET_BACKEND: JVM

// WITH_STDLIB

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class UpdateableThing {
    private val lock = ReentrantReadWriteLock()
    private var updateCount = 0

    fun <T> performUpdates(block: () -> T): T {
        lock.write {
            ++updateCount
            val result = block()
            --updateCount

            return result
        }
    }
}


fun box(): String {
    return UpdateableThing().performUpdates { "OK" }
}

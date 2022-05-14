package language

import java.lang.Double as jDouble
import java.lang.Float as jFloat
import org.junit.Test as test
import kotlin.test.*

public class RangeJVMTest {

    @test fun doubleRange() {
        val range = -1.0..3.14159265358979
        assertFalse(jDouble.NEGATIVE_INFINITY in range)
        assertFalse(jDouble.POSITIVE_INFINITY in range)
        assertFalse(jDouble.NaN in range)
    }

    @test fun floatRange() {
        val range = -1.0f..3.14159f
        assertFalse(jFloat.NEGATIVE_INFINITY in range)
        assertFalse(jFloat.POSITIVE_INFINITY in range)

        assertFalse(jFloat.NaN in range)
    }

    @test fun illegalProgressionCreation() {
        fun assertFailsWithIllegalArgument(f: () -> Unit) = assertFailsWith(IllegalArgumentException::class, block = f)
        // create Progression explicitly with increment = 0
        assertFailsWithIllegalArgument { IntProgression(0, 5, 0) }
        assertFailsWithIllegalArgument { ByteProgression(0, 5, 0) }
        assertFailsWithIllegalArgument { ShortProgression(0, 5, 0) }
        assertFailsWithIllegalArgument { LongProgression(0, 5, 0) }
        assertFailsWithIllegalArgument { CharProgression('a', 'z', 0) }
        assertFailsWithIllegalArgument { DoubleProgression(0.0, 5.0, 0.0) }
        assertFailsWithIllegalArgument { FloatProgression(0.0f, 5.0f, 0.0f) }

        assertFailsWithIllegalArgument { 0..5 step 0 }
        assertFailsWithIllegalArgument { 0.toByte()..5.toByte() step 0 }
        assertFailsWithIllegalArgument { 0.toShort()..5.toShort() step 0  }
        assertFailsWithIllegalArgument { 0L..5L step 0L }
        assertFailsWithIllegalArgument { 'a'..'z' step 0 }
        assertFailsWithIllegalArgument { 0.0..5.0 step 0.0 }
        assertFailsWithIllegalArgument { 0.0f..5.0f step 0.0f }

        assertFailsWithIllegalArgument { 0 downTo -5 step 0 }
        assertFailsWithIllegalArgument { 0.toByte() downTo -5.toByte() step 0 }
        assertFailsWithIllegalArgument { 0.toShort() downTo -5.toShort() step 0  }
        assertFailsWithIllegalArgument { 0L downTo -5L step 0L }
        assertFailsWithIllegalArgument { 'z' downTo 'a' step 0 }
        assertFailsWithIllegalArgument { 0.0 downTo -5.0 step 0.0 }
        assertFailsWithIllegalArgument { 0.0f downTo -5.0f step 0.0f }

        assertFailsWithIllegalArgument { 0..5 step -2 }
        assertFailsWithIllegalArgument { 0.toByte()..5.toByte() step -2 }
        assertFailsWithIllegalArgument { 0.toShort()..5.toShort() step -2  }
        assertFailsWithIllegalArgument { 0L..5L step -2L }
        assertFailsWithIllegalArgument { 'a'..'z' step -2 }
        assertFailsWithIllegalArgument { 0.0..5.0 step -0.5 }
        assertFailsWithIllegalArgument { 0.0f..5.0f step -0.5f }

        assertFailsWithIllegalArgument { 0 downTo -5 step -2 }
        assertFailsWithIllegalArgument { 0.toByte() downTo -5.toByte() step -2 }
        assertFailsWithIllegalArgument { 0.toShort() downTo -5.toShort() step -2  }
        assertFailsWithIllegalArgument { 0L downTo -5L step -2L }
        assertFailsWithIllegalArgument { 'z' downTo 'a' step -2 }
        assertFailsWithIllegalArgument { 0.0 downTo -5.0 step -0.5 }
        assertFailsWithIllegalArgument { 0.0f downTo -5.0f step -0.5f }

        // NaN increment or step
        assertFailsWithIllegalArgument { DoubleProgression(0.0, 5.0, jDouble.NaN) }
        assertFailsWithIllegalArgument { FloatProgression(0.0f, 5.0f, jFloat.NaN) }

        assertFailsWithIllegalArgument { 0.0..5.0 step jDouble.NaN }
        assertFailsWithIllegalArgument { 0.0f..5.0f step jFloat.NaN }

        assertFailsWithIllegalArgument { 5.0 downTo 0.0 step jDouble.NaN }
        assertFailsWithIllegalArgument { 5.0f downTo 0.0f step jFloat.NaN }
    }
}
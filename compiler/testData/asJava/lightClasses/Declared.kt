package test

class NoModifiers

// Visibility
public class Public
private class Private
internal class Internal
class Outer {
    public class Public
    protected class Protected
    private class Private
    internal class Internal

    inner class Inner
}

// Modality
abstract class Abstract
open class Open
final class Final

// Special
annotation class Annotation
enum class Enum
trait Trait

// Deprecation
deprecated("") class Deprecated
jet.deprecated("") class DeprecatedFQN
jet. deprecated /**/ ("") class DeprecatedFQNSpaces
[deprecated("")] class DeprecatedWithBrackets
[jet.deprecated("")] class DeprecatedWithBracketsFQN
[jet
./**/deprecated  ("")] class DeprecatedWithBracketsFQNSpaces

// Generic
class Generic1<T>
class Generic2<A, B>


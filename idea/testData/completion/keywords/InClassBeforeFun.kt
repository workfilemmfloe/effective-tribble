public class Test {

    <caret>

    fun test() {

    }
}

// EXIST:  abstract
// ?ABSENT: annotation
// ABSENT: as
// ABSENT: break
// ABSENT: by
// ABSENT: catch
// EXIST:  class ... {...}
// ABSENT: continue
// ABSENT: default
// ABSENT: do
// ABSENT: else
// EXIST:  enum class ... {...}
// ABSENT: false
// EXIST:  final
// ABSENT: finally
// ABSENT: for
// EXIST:  fun ...(...) : ... {...}
// EXIST:  get
// ABSENT: if
// ABSENT: import
// ABSENT: in
// EXIST:  inline
// EXIST:  internal
// ABSENT: is
// ABSENT: null
// EXIST:  object
// EXIST:  open
// ABSENT: out
// EXIST:  override
// ABSENT: package
// EXIST:  private
// EXIST:  protected
// EXIST:  public
// ABSENT: ref
// ABSENT: return
// EXIST:  set
// ABSENT: super
// ABSENT: This
// ABSENT: this
// ABSENT: throw
// EXIST:  trait ... {...}
// ABSENT: true
// ABSENT: try
// EXIST:  type
// EXIST:  val ... : ... = ...
// EXIST:  val ... : ...get() {...}
// EXIST:  var ... : ... = ...
// EXIST:  var ... : ...get() {...}set(value) {...}
// ABSENT: vararg
// ABSENT: when
// ABSENT: where
// ABSENT: while
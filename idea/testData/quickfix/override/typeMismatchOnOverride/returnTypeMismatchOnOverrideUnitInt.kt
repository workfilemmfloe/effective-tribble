// "Remove explicitly specified return type in 'remove' function" "true"
abstract class A : java.util.Iterator<Int> {
    public abstract override fun remove() : Int<caret>;
}

package test

class Klass {
    companion object {
        // Old and new constant values are different, but their hashes are the same
        @JvmField
        val CONST = "BF"
    }
}

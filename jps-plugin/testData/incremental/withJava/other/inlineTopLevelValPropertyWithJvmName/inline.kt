package inline

inline val f: Int
    @JvmName("getG") get() = 0

@file:JvmVersion
@file:JvmMultifileClass
@file:JvmName("FilesKt")
package kotlin.io

import java.io.File
import java.io.IOException
import java.util.Stack
import kotlin.support.AbstractIterator

/**
 * An enumeration to describe possible walk directions.
 * There are two of them: beginning from parents, ending with children,
 * and beginning from children, ending with parents. Both use depth-first search.
 */
public enum class FileWalkDirection {
    /** Depth-first search, directory is visited BEFORE its files */
    TOP_DOWN,
    /** Depth-first search, directory is visited AFTER its files */
    BOTTOM_UP
    // Do we want also breadth-first search?
}

/**
 * This class is intended to implement different file walk methods.
 * It allows to iterate through all files inside [start] directory.
 * If [start] is just a file, walker iterates only it.
 * If [start] does not exist, walker does not do any iterations at all.
 *
 * @param start directory to walk into.
 * @param direction selects top-down or bottom-up order (in other words, parents first or children first).
 * @param onEnter is called on any entered directory before its files are visited and before it is visited itself,
 *  if `false` is returned, directory is not visited entirely.
 * @param onLeave is called on any left directory after its files are visited and after it is visited itself.
 * @param onFail is called on a directory when it's impossible to get its file list.
 * @param filter is called just before visiting a file, and if `false` is returned, file is not visited.
 * @param maxDepth is maximum walking depth, it must be positive. With a value of 1,
 * walker visits [start] and all its children, with a value of 2 also grandchildren, etc.
 */
public class FileTreeWalk private constructor(
        private val start: File,
        private val direction: FileWalkDirection = FileWalkDirection.TOP_DOWN,
        private val onEnter: ((File) -> Boolean)?,
        private val onLeave: ((File) -> Unit)?,
        private val onFail: ((f: File, e: IOException) -> Unit)?,
        private val filter: (File) -> Boolean = { true },
        private val maxDepth: Int = Int.MAX_VALUE,
        dummy: Boolean = false
) : Sequence<File> {

    internal constructor(start: File, direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): this(start, direction, null, null, null, dummy = false)

/*
    private constructor(
        start: File,
        direction: FileWalkDirection = FileWalkDirection.TOP_DOWN,
        onEnter: ((File) -> Boolean)? = null,
        onLeave: ((File) -> Unit)? = null,
        onFail: ((f: File, e: IOException) -> Unit)? = null,
        maxDepth: Int = Int.MAX_VALUE
        ) : this(start, direction, onEnter, onLeave, onFail, maxDepth = maxDepth, dummy = false)
*/

    @Deprecated("Use builder methods on an instance obtained from File.walk/walkTopDown/walkBottomUp instead of directly calling constructor.")
    public constructor(
        start: File,
        direction: FileWalkDirection = FileWalkDirection.TOP_DOWN,
        enter: (File) -> Unit = {},
        leave: (File) -> Unit = {},
        fail: (f: File, e: IOException) -> Unit = { f, e -> Unit },
        filter: (File) -> Boolean = { true },
        maxDepth: Int = Int.MAX_VALUE
    ) : this(start, direction, onEnter = { enter(it); true }, onLeave = leave, onFail = fail, filter = filter, maxDepth = maxDepth)

    /** Returns an iterator walking through files. */
    override public fun iterator(): Iterator<File> = FileTreeWalkIterator()

    /** Abstract class that encapsulates file visiting in some order, beginning from a given [root] */
    private abstract class WalkState(val root: File) {
        /** Call of this function proceeds to a next file for visiting and returns it */
        abstract public fun step(): File?
    }

    /** Abstract class that encapsulates directory visiting in some order, beginning from a given [rootDir] */
    private abstract class DirectoryState(rootDir: File): WalkState(rootDir) {
        init {
            @Suppress("DEPRECATION")
            if (ASSERTIONS_ENABLED)
                assert(rootDir.isDirectory) { "rootDir must be verified to be directory beforehand." }
        }
    }

    private inner class FileTreeWalkIterator : AbstractIterator<File>() {

        // Stack of directory states, beginning from the start directory
        private val state = Stack<WalkState>()

        init {
            if (start.isDirectory && filter(start)) {
                state.push(directoryState(start))
            } else if (start.isFile) {
                state.push(SingleFileState(start))
            } else {
                done()
            }

        }

        override fun computeNext() {
            val nextFile = gotoNext()
            if (nextFile != null)
                setNext(nextFile)
            else
                done()
        }


        private fun directoryState(root: File): DirectoryState {
            return when (direction) {
                FileWalkDirection.TOP_DOWN -> TopDownDirectoryState(root)
                FileWalkDirection.BOTTOM_UP -> BottomUpDirectoryState(root)
            }
        }

        tailrec private fun gotoNext(): File? {

            if (state.empty()) {
                // There is nothing in the state
                return null
            }
            // Take next file from the top of the stack
            val topState = state.peek()!!
            val file = topState.step()
            if (file == null) {
                // There is nothing more on the top of the stack, go back
                state.pop()
                return gotoNext()
            } else {
                // Check that file/directory matches the filter
                if (!filter(file))
                    return gotoNext()
                if (file == topState.root || !file.isDirectory || state.size >= maxDepth) {
                    // Proceed to a root directory or a simple file
                    return file
                } else {
                    // Proceed to a sub-directory
                    state.push(directoryState(file))
                    return gotoNext()
                }
            }
        }

        /** Visiting in bottom-up order */
        private inner class BottomUpDirectoryState(rootDir: File) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var fileList: Array<File>? = null

            private var fileIndex = 0

            private var failed = false

            /** First all children, then root directory */
            override public fun step(): File? {
                if (!failed && fileList == null) {
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    fileList = root.listFiles()
                    if (fileList == null) {
                        onFail?.invoke(root, AccessDeniedException(file = root, reason = "Cannot list files in a directory"))
                        failed = true
                    }
                }
                if (fileList != null && fileIndex < fileList!!.size) {
                    // First visit all files
                    return fileList!![fileIndex++]
                } else if (!rootVisited) {
                    // Then visit root
                    rootVisited = true
                    return root
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        /** Visiting in top-down order */
        private inner class TopDownDirectoryState(rootDir: File) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var fileList: Array<File>? = null

            private var fileIndex = 0

            /** First root directory, then all children */
            override public fun step(): File? {
                if (!rootVisited) {
                    // First visit root
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    rootVisited = true
                    return root
                } else if (fileList == null || fileIndex < fileList!!.size) {
                    if (fileList == null) {
                        // Then read an array of files, if any
                        fileList = root.listFiles()
                        if (fileList == null) {
                            onFail?.invoke(root, AccessDeniedException(file = root, reason = "Cannot list files in a directory"))
                        }
                        if (fileList == null || fileList!!.size == 0) {
                            onLeave?.invoke(root)
                            return null
                        }
                    }
                    // Then visit all files
                    return fileList!![fileIndex++]
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        private inner class SingleFileState(rootFile: File) : WalkState(rootFile) {
            private var visited: Boolean = false

            init {
                @Suppress("DEPRECATION")
                if (ASSERTIONS_ENABLED)
                    assert(rootFile.isFile) { "rootFile must be verified to be file beforehand." }
            }

            override fun step(): File? {
                if (visited) return null
                visited = true
                if (!filter(root)) return null
                return root
            }
        }

    }

    /**
     * Sets enter directory predicate [function].
     * Enter [function] is called BEFORE the corresponding directory and its files are visited.
     * If the [function] returns `false` the directory is not entered, and neither it nor its files are not visited.
     */
    public fun onEnter(function: (File) -> Boolean): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter = function, onLeave = onLeave, onFail = onFail, filter = filter, maxDepth = maxDepth)
    }

    @Deprecated("Use onEnter instead.")
    public fun enter(function: (File) -> Unit): FileTreeWalk = onEnter { function(it); true }

    /**
     * Sets leave directory [function].
     * Leave [function] is called AFTER the corresponding directory and its files are visited.
     */
    public fun onLeave(function: (File) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter, function, onFail, filter, maxDepth, false)
    }

    @Deprecated("Use onLeave instead.", ReplaceWith("onLeave(function)"))
    public fun leave(function: (File) -> Unit): FileTreeWalk = onLeave(function)

    /**
     * Set fail entering directory [function].
     * Fail [function] is called when walker is unable to get list of directory files.
     * Enter and leave functions are called even in this case.
     */
    public fun onFail(function: (File, IOException) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter, onLeave, function, filter, maxDepth)
    }

    @Deprecated("Use onFail instead.", ReplaceWith("onFail(function)"))
    public fun fail(function: (File, IOException) -> Unit): FileTreeWalk = onFail(function)


    /**
     * Sets tree filter [predicate].
     * Tree filter [predicate] function is called before visiting files and entering directories.
     * If it returns `false`, file is not visited, directory is not entered and all its content is also not visited.
     * If it returns `true`, everything goes in the regular way.
     */
    @Deprecated("Filter out directories entirely with onEnter, and all items with filter().")
    public fun treeFilter(predicate: (File) -> Boolean): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter, onLeave, onFail, predicate, maxDepth)
    }

    /**
     * Sets maximum [depth] of walk. Int.MAX_VALUE is used for unlimited.
     * Negative and zero values are not allowed.
     */
    public fun maxDepth(depth: Int): FileTreeWalk {
        if (depth <= 0)
            throw IllegalArgumentException("Use positive depth value")
        return FileTreeWalk(start, direction, onEnter, onLeave, onFail, filter, depth)
    }
}

/**
 * Gets a sequence for visiting this directory and all its content.
 *
 * @param direction walk direction, top-down (by default) or bottom-up.
 */
public fun File.walk(direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): FileTreeWalk =
        FileTreeWalk(this, direction)

/**
 * Gets a sequence for visiting this directory and all its content in top-down order.
 * Depth-first search is used and directories are visited before all their files.
 */
public fun File.walkTopDown(): FileTreeWalk = walk(FileWalkDirection.TOP_DOWN)

/**
 * Gets a sequence for visiting this directory and all its content in bottom-up order.
 * Depth-first search is used and directories are visited after all their files.
 */
public fun File.walkBottomUp(): FileTreeWalk = walk(FileWalkDirection.BOTTOM_UP)

/**
 * Recursively process this file and all children with the given block.
 * Note that if this file doesn't exist, then the block will be executed on it anyway.
 *
 * @param function the function to call on each file.
 */
@Deprecated("It's recommended to use walkTopDown() / walkBottomUp()", ReplaceWith("walkTopDown().forEach(function)"), DeprecationLevel.ERROR)
public fun File.recurse(function: (File) -> Unit): Unit {
    walkTopDown().forEach(function)
}

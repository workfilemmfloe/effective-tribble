/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin

import java.util.*
import java.util.concurrent.Callable

deprecated("Use sortedSetOf(...) instead", ReplaceWith("sortedSetOf(*values)"))
public fun sortedSet<T>(vararg values: T): TreeSet<T> = sortedSetOf(*values)

deprecated("Use sortedSetOf(...) instead", ReplaceWith("sortedSetOf(comparator, *values)"))
public fun sortedSet<T>(comparator: Comparator<T>, vararg values: T): TreeSet<T> = sortedSetOf(comparator, *values)

deprecated("Use sortedMapOf(...) instead", ReplaceWith("sortedMapOf(*values)"))
public fun <K, V> sortedMap(vararg values: Pair<K, V>): SortedMap<K, V> = sortedMapOf(*values)

/**
 * A helper method for creating a [[Callable]] from a function
 */
deprecated("Use SAM constructor: Callable(...)", ReplaceWith("Callable(action)", "java.util.concurrent.Callable"))
public /*inline*/ fun <T> callable(action: () -> T): Callable<T> = Callable(action)

deprecated("Use length() instead", ReplaceWith("length()"))
public val String.size: Int
    get() = length()

deprecated("Use length() instead", ReplaceWith("length()"))
public val CharSequence.size: Int
    get() = length()


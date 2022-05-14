/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

public abstract class Visibility protected constructor(
        public val name: String,
        public val isPublicAPI: Boolean
) {
    public abstract fun isVisible(receiver: ReceiverValue, what: DeclarationDescriptorWithVisibility, from: DeclarationDescriptor): Boolean

    /**
     * True, if it makes sense to check this visibility in imports and not import inaccessible declarations with such visibility.
     * Hint: return true, if this visibility can be checked on file's level.
     * Examples:
     * it returns false for PROTECTED because protected members of classes can be imported to be used in subclasses of their containers,
     * so when we are looking at the import, we don't know whether it is legal somewhere in this file or not.
     * it returns true for INTERNAL, because an internal declaration is either visible everywhere in a file, or invisible everywhere in the same file.
     * it returns true for PRIVATE, because there's no point in importing privates: they are inaccessible unless their short name is
     * already available without an import
     */
    public abstract fun mustCheckInImports(): Boolean

    /**
     * @return null if the answer is unknown
     */
    protected open fun compareTo(visibility: Visibility): Int? {
        return Visibilities.compareLocal(this, visibility)
    }

    public open val displayName: String
        get() = name

    override final fun toString() = displayName

    public open fun normalize(): Visibility = this
}

/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package jet;

import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

import java.util.Iterator;

/**
 * @author alex.tkachman
 */
@AssertInvisibleInResolver
public abstract class BooleanIterator implements Iterator<Boolean> {
    @Override
    public final Boolean next() {
        return nextBoolean();
    }

    public abstract boolean nextBoolean();

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Mutating method called on a Kotlin Iterator");
    }
}

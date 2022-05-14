/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.context.generator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.List;
import java.util.Map;

public class Generator<V> {


    @NotNull
    private final Map<DeclarationDescriptor, V> values = Maps.newHashMap();
    @NotNull
    private final List<Rule<V>> rules = Lists.newArrayList();

    public void addRule(@NotNull Rule<V> rule) {
        rules.add(rule);
    }

    @Nullable
    public V get(@NotNull DeclarationDescriptor descriptor) {
        V result = values.get(descriptor);
        if (result != null) {
            return result;
        }
        result = generate(descriptor);
        values.put(descriptor, result);
        return result;
    }

    @Nullable
    private V generate(@NotNull DeclarationDescriptor descriptor) {
        V result = null;
        for (Rule<V> rule : rules) {
            result = rule.apply(descriptor);
            if (result != null) {
                return result;
            }
        }
        return result;
    }
}

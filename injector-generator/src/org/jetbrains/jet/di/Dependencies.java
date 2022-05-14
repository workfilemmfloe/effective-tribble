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

package org.jetbrains.jet.di;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

import static org.jetbrains.jet.di.InjectorGeneratorUtil.var;

public class Dependencies {

    private final Set<Field> allFields = Sets.newLinkedHashSet();
    private final Set<Field> satisfied = Sets.newHashSet();
    private final Set<Field> used = Sets.newHashSet();
    private final Multimap<DiType, Field> typeToFields = HashMultimap.create();

    private final Set<Field> newFields = Sets.newLinkedHashSet();

    public void addField(@NotNull Field field) {
        allFields.add(field);
        typeToFields.put(field.getType(), field);
    }

    public void addSatisfiedField(@NotNull Field field) {
        addField(field);
        satisfied.add(field);
    }

    private Field addNewField(@NotNull DiType type) {
        Field field = Field.create(false, type, var(type), null);
        addField(field);
        newFields.add(field);
        return field;
    }

    private void satisfyDependenciesFor(Field field, Chain<Field> neededFor) {
        if (!satisfied.add(field)) return;

        Expression initialization = field.getInitialization();
        if (initialization instanceof InstantiateType) {
            initializeByConstructorCall(field, neededFor);
        }
        DiType typeToInitialize = InjectorGeneratorUtil.getEffectiveFieldType(field);

        // Sort setters in order to get deterministic behavior
        List<Method> declaredMethods = Lists.newArrayList(typeToInitialize.getClazz().getDeclaredMethods());
        Collections.sort(declaredMethods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (Method method : declaredMethods) {
            if (method.getAnnotation(javax.inject.Inject.class) == null
                || !method.getName().startsWith("set")
                || method.getParameterTypes().length != 1) continue;

            Type parameterType = method.getGenericParameterTypes()[0];


            Field dependency = findDependencyOfType(
                    DiType.fromReflectionType(parameterType),
                    field + ": " + method + ": " + allFields,
                    neededFor.prepend(field)
            );
            used.add(dependency);

            field.getDependencies().add(new SetterDependency(field, method.getName(), dependency));
        }
    }

    private Field findDependencyOfType(DiType parameterType, String errorMessage, Chain<Field> neededFor) {
        List<Field> fields = Lists.newArrayList();
        for (Map.Entry<DiType, Field> entry : typeToFields.entries()) {
            if (parameterType.isAssignableFrom(entry.getKey())) {
                fields.add(entry.getValue());
            }
        }

        if (fields.isEmpty()) {

            if (parameterType.getClazz().isPrimitive() || parameterType.getClazz().getPackage().getName().equals("java.lang")) {
                throw new IllegalArgumentException(
                        "cannot declare magic field of type " + parameterType + ": " + errorMessage);
            }

            Field dependency = addNewField(parameterType);
            satisfyDependenciesFor(dependency, neededFor);
            return dependency;
        }
        else if (fields.size() == 1) {
            return fields.iterator().next();
        }
        else {
            throw new IllegalArgumentException("Ambiguous dependency: \n"
                                               + errorMessage
                                               + "\nneeded for " + neededFor
                                               + "\navailable: " + fields);
        }
    }

    private void initializeByConstructorCall(Field field, Chain<Field> neededFor) {
        Expression initialization = field.getInitialization();
        DiType type = ((InstantiateType) initialization).getType();

        if (type.getClazz().isInterface()) {
            throw new IllegalArgumentException("cannot instantiate interface: " + type.getClazz().getName() + " needed for " + neededFor);
        }
        if (Modifier.isAbstract(type.getClazz().getModifiers())) {
            throw new IllegalArgumentException("cannot instantiate abstract class: " + type.getClazz().getName() + " needed for " + neededFor);
        }

        // Note: projections are not computed here

        // Look for constructor
        Constructor<?>[] constructors = type.getClazz().getConstructors();
        if (constructors.length == 0 || !Modifier.isPublic(constructors[0].getModifiers())) {
            throw new IllegalArgumentException("No constructor: " + type.getClazz().getName() + " needed for " + neededFor);
        }
        if (constructors.length > 1) {
            throw new IllegalArgumentException("Too many constructors in " + type.getClazz().getName() + " needed for " + neededFor);
        }
        Constructor<?> constructor = constructors[0];

        // Find arguments
        ConstructorCall dependency = new ConstructorCall(constructor);
        Type[] parameterTypes = constructor.getGenericParameterTypes();
        for (Type parameterType : parameterTypes) {
            Field fieldForParameter = findDependencyOfType(
                    DiType.fromReflectionType(parameterType),
                    "constructor: " + constructor + ", parameter: " + parameterType,
                    neededFor.prepend(field)
            );
            used.add(fieldForParameter);
            dependency.getConstructorArguments().add(fieldForParameter);
        }

        field.setInitialization(dependency);
    }

    public Collection<Field> satisfyDependencies() {
        for (Field field : Lists.newArrayList(allFields)) {
            satisfyDependenciesFor(field, ImmutableList.<Field>empty());
        }
        return newFields;
    }

    @NotNull
    public Set<Field> getUsedFields() {
        return used;
    }

    private interface Chain<T> {
        @NotNull
        Chain<T> prepend(T t);
    }

    private static class ImmutableList<T> implements Chain<T> {

        private static final Chain EMPTY = new Chain() {
            @NotNull
            @Override
            public Chain prepend(Object o) {
                return create(o);
            }

            @Override
            public String toString() {
                return "<itself>";
            }
        };

        @NotNull
        public static <T> Chain<T> empty() {
            return EMPTY;
        }

        @NotNull
        public static <T> ImmutableList<T> create(@NotNull T t) {
            return new ImmutableList<T>(t, ImmutableList.<T>empty());
        }

        private final T head;
        private final Chain<T> tail;

        private ImmutableList(@NotNull T head, @NotNull Chain<T> tail) {
            this.head = head;
            this.tail = tail;
        }

        @NotNull
        @Override
        public ImmutableList<T> prepend(@NotNull T t) {
            return new ImmutableList<T>(t, this);
        }

        @Override
        public String toString() {
            return doToString(this, new StringBuilder()).toString();
        }

        private static <T> CharSequence doToString(@NotNull Chain<T> chain, StringBuilder builder) {
            if (chain == empty()) {
                builder.append("|");
                return builder;
            }

            ImmutableList<T> list = (ImmutableList<T>) chain;
            builder.append("\n\t").append(list.head).append(" -> ");
            return doToString(list.tail, builder);
        }
    }

}

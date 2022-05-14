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

package kotlin.jvm.internal;

import kotlin.Function;
import kotlin.jvm.functions.*;
import kotlin.jvm.internal.markers.*;

import java.util.*;

@SuppressWarnings("unused")
public class TypeIntrinsics {
    private static <T extends Throwable> T sanitizeStackTrace(T throwable) {
        return Intrinsics.sanitizeStackTrace(throwable, TypeIntrinsics.class.getName());
    }

    public static void throwCce(Object argument, String requestedClassName) {
        String argumentClassName = argument == null ? "null" : argument.getClass().getName();
        throw sanitizeStackTrace(new ClassCastException(argumentClassName + " cannot be cast to " + requestedClassName));
    }

    public static void throwCce(ClassCastException e) {
        throw Intrinsics.sanitizeStackTrace(e, TypeIntrinsics.class.getName());
    }

    public static boolean isMutableIterator(Object obj) {
        return (obj instanceof Iterator) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableIterator));
    }

    public static Iterator asMutableIterator(Object obj) {
        Iterator result = null;
        try {
            result = (Iterator) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterator)) {
            throwCce(obj, "kotlin.MutableIterator");
        }
        return result;
    }

    public static boolean isMutableListIterator(Object obj) {
        return (obj instanceof ListIterator) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableListIterator));
    }

    public static ListIterator asMutableListIterator(Object obj) {
        ListIterator result = null;
        try {
            result = (ListIterator) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableListIterator)) {
            throwCce(obj, "kotlin.MutableListIterator");
        }
        return result;
    }

    public static boolean isMutableIterable(Object obj) {
        return (obj instanceof Iterable) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableIterable));
    }

    public static Iterable asMutableIterable(Object obj) {
        Iterable result = null;
        try {
            result = (Iterable) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableIterable)) {
            throwCce(obj, "kotlin.MutableIterable");
        }
        return result;
    }

    public static boolean isMutableCollection(Object obj) {
        return (obj instanceof Collection) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableCollection));
    }

    public static Collection asMutableCollection(Object obj) {
        Collection result = null;
        try {
            result = (Collection) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableCollection)) {
            throwCce(obj, "kotlin.MutableCollection");
        }
        return result;
    }

    public static boolean isMutableList(Object obj) {
        return (obj instanceof List) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableList));
    }

    public static List asMutableList(Object obj) {
        List result = null;
        try {
            result = (List) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableList)) {
            throwCce(obj, "kotlin.MutableList");
        }
        return result;
    }

    public static boolean isMutableSet(Object obj) {
        return (obj instanceof Set) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableSet));
    }

    public static Set asMutableSet(Object obj) {
        Set result = null;
        try {
            result = (Set) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableSet)) {
            throwCce(obj, "kotlin.MutableSet");
        }
        return result;
    }

    public static boolean isMutableMap(Object obj) {
        return (obj instanceof Map) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableMap));
    }

    public static Map asMutableMap(Object obj) {
        Map result = null;
        try {
            result = (Map) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap)) {
            throwCce(obj, "kotlin.MutableMap");
        }
        return result;
    }

    public static boolean isMutableMapEntry(Object obj) {
        return (obj instanceof Map.Entry) &&
               (!(obj instanceof KMappedMarker) || (obj instanceof KMutableMap.Entry));
    }

    public static Map.Entry asMutableMapEntry(Object obj) {
        Map.Entry result = null;
        try {
            result = (Map.Entry) obj;
        }
        catch (ClassCastException e) {
            throwCce(e);
        }
        if ((obj instanceof KMappedMarker) && !(obj instanceof KMutableMap.Entry)) {
            throwCce(obj, "kotlin.MutableMap.MutableEntry");
        }
        return result;
    }

    public static int getFunctionArity(Object obj) {
        if (obj instanceof FunctionImpl) {
            return ((FunctionImpl) obj).getArity();
        }
        else if (obj instanceof Function0) {
            return 0;
        }
        else if (obj instanceof Function1) {
            return 1;
        }
        else if (obj instanceof Function2) {
            return 2;
        }
        else if (obj instanceof Function3) {
            return 3;
        }
        else if (obj instanceof Function4) {
            return 4;
        }
        else if (obj instanceof Function5) {
            return 5;
        }
        else if (obj instanceof Function6) {
            return 6;
        }
        else if (obj instanceof Function7) {
            return 7;
        }
        else if (obj instanceof Function8) {
            return 8;
        }
        else if (obj instanceof Function9) {
            return 9;
        }
        else if (obj instanceof Function10) {
            return 10;
        }
        else if (obj instanceof Function11) {
            return 11;
        }
        else if (obj instanceof Function12) {
            return 12;
        }
        else if (obj instanceof Function13) {
            return 13;
        }
        else if (obj instanceof Function14) {
            return 14;
        }
        else if (obj instanceof Function15) {
            return 15;
        }
        else if (obj instanceof Function16) {
            return 16;
        }
        else if (obj instanceof Function17) {
            return 17;
        }
        else if (obj instanceof Function18) {
            return 18;
        }
        else if (obj instanceof Function19) {
            return 19;
        }
        else if (obj instanceof Function20) {
            return 20;
        }
        else if (obj instanceof Function21) {
            return 21;
        }
        else if (obj instanceof Function22) {
            return 22;
        }
        else {
            return -1;
        }
    }

    public static boolean isFunctionOfArity(Object obj, int arity) {
        return obj instanceof Function && getFunctionArity(obj) == arity;
    }

    public static Object beforeCheckcastToFunctionOfArity(Object obj, int arity) {
        // TODO should we instead inline bytecode for this in TypeIntrinsics.kt?
        if (obj != null && !isFunctionOfArity(obj, arity)) {
            throwCce(obj, "kotlin.jvm.functions.Function" + arity);
        }
        return obj;
    }

    public static Object beforeSafeCheckcastToFunctionOfArity(Object obj, int arity) {
        // TODO should we instead inline bytecode for this in TypeIntrinsics.kt?
        if (obj == null || !isFunctionOfArity(obj, arity)) {
            return null;
        }
        return obj;
    }
}

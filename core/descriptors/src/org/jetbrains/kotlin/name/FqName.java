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

package org.jetbrains.kotlin.name;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.utils.StringsKt;

import java.util.ArrayList;
import java.util.List;

public final class FqName extends FqNameBase {

    @NotNull
    public static FqName fromSegments(@NotNull List<String> names) {
        return new FqName(StringsKt.join(names, "."));
    }

    public static final FqName ROOT = new FqName("");

    @NotNull
    private final FqNameUnsafe fqName;

    // cache
    private transient FqName parent;

    public FqName(@NotNull String fqName) {
        this.fqName = new FqNameUnsafe(fqName, this);
    }

    public FqName(@NotNull FqNameUnsafe fqName) {
        this.fqName = fqName;
    }

    private FqName(@NotNull FqNameUnsafe fqName, FqName parent) {
        this.fqName = fqName;
        this.parent = parent;
    }

    /*package*/ static boolean isValidAfterUnsafeCheck(@NotNull String qualifiedName) {
        // TODO: There's a valid name with escape char ``
        return qualifiedName.indexOf('<') < 0;
    }

    @Override
    @NotNull
    public String asString() {
        return fqName.asString();
    }

    @NotNull
    public FqNameUnsafe toUnsafe() {
        return fqName;
    }

    public boolean isRoot() {
        return fqName.isRoot();
    }

    @NotNull
    public FqName parent() {
        if (parent != null) {
            return parent;
        }

        if (isRoot()) {
            throw new IllegalStateException("root");
        }

        parent = new FqName(fqName.parent());

        return parent;
    }

    @NotNull
    public FqName child(@NotNull Name name) {
        return new FqName(fqName.child(name), this);
    }

    @NotNull
    public Name shortName() {
        return fqName.shortName();
    }

    @Override
    @NotNull
    public Name shortNameOrSpecial() {
        return fqName.shortNameOrSpecial();
    }

    @NotNull
    public List<FqName> path() {
        final List<FqName> path = new ArrayList<FqName>();
        path.add(ROOT);
        fqName.walk(new FqNameUnsafe.WalkCallback() {
            @Override
            public void segment(@NotNull Name shortName, @NotNull FqNameUnsafe fqName) {
                // TODO: do not validate
                path.add(new FqName(fqName));
            }
        });
        return path;
    }

    @Override
    @NotNull
    public List<Name> pathSegments() {
        return fqName.pathSegments();
    }

    public boolean firstSegmentIs(@NotNull Name segment) {
        return fqName.firstSegmentIs(segment);
    }

    public boolean lastSegmentIs(@NotNull Name segment) {
        return fqName.lastSegmentIs(segment);
    }

    public boolean isAncestorOf(@NotNull FqName other) {
        String thisString = this.asString();
        String otherString = other.asString();
        return otherString.equals(thisString) || otherString.startsWith(thisString + ".");
    }

    @NotNull
    public static FqName topLevel(@NotNull Name shortName) {
        return new FqName(FqNameUnsafe.topLevel(shortName));
    }


    @Override
    public String toString() {
        return fqName.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FqName)) return false;

        FqName otherFqName = (FqName) o;

        if (!fqName.equals(otherFqName.fqName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return fqName.hashCode();
    }
}

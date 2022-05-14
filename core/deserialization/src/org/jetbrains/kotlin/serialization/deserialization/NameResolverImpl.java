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

package org.jetbrains.kotlin.serialization.deserialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import static org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName;

public class NameResolverImpl implements NameResolver {
    @NotNull
    public static NameResolverImpl read(@NotNull InputStream in) {
        try {
            ProtoBuf.StringTable simpleNames = ProtoBuf.StringTable.parseDelimitedFrom(in);
            ProtoBuf.QualifiedNameTable qualifiedNames = ProtoBuf.QualifiedNameTable.parseDelimitedFrom(in);
            return new NameResolverImpl(simpleNames, qualifiedNames);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private final ProtoBuf.StringTable strings;
    private final ProtoBuf.QualifiedNameTable qualifiedNames;

    private NameResolverImpl(
            @NotNull ProtoBuf.StringTable strings,
            @NotNull ProtoBuf.QualifiedNameTable qualifiedNames
    ) {
        this.strings = strings;
        this.qualifiedNames = qualifiedNames;
    }

    @Override
    @NotNull
    public String getString(int index) {
        return strings.getString(index);
    }

    @Override
    @NotNull
    public Name getName(int index) {
        return Name.guess(strings.getString(index));
    }

    @Override
    @NotNull
    public ClassId getClassId(int index) {
        LinkedList<String> packageFqName = new LinkedList<String>();
        LinkedList<String> relativeClassName = new LinkedList<String>();
        boolean local = false;

        while (index != -1) {
            QualifiedName proto = qualifiedNames.getQualifiedName(index);
            String shortName = strings.getString(proto.getShortName());
            switch (proto.getKind()) {
                case CLASS:
                    relativeClassName.addFirst(shortName);
                    break;
                case PACKAGE:
                    packageFqName.addFirst(shortName);
                    break;
                case LOCAL:
                    relativeClassName.addFirst(shortName);
                    local = true;
                    break;
            }

            index = proto.getParentQualifiedName();
        }

        return new ClassId(FqName.fromSegments(packageFqName), FqName.fromSegments(relativeClassName), local);
    }
}

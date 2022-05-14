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

package org.jetbrains.jet.lang.types.lang;

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext;
import org.jetbrains.jet.descriptors.serialization.descriptors.*;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class BuiltinsPackageFragment extends PackageFragmentDescriptorImpl {
    private final DeserializedPackageMemberScope members;
    private final NameResolver nameResolver;
    private final PackageFragmentProvider packageFragmentProvider;

    public BuiltinsPackageFragment(@NotNull StorageManager storageManager, @NotNull ModuleDescriptor module) {
        super(module, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
        nameResolver = NameSerializationUtil.deserializeNameResolver(getStream(BuiltInsSerializationUtil.getNameTableFilePath(getFqName())));

        packageFragmentProvider = new BuiltinsPackageFragmentProvider();

        DeserializationContext context = new DeserializationContext(
                storageManager, new BuiltInsDescriptorFinder(storageManager),
                // TODO: support annotations
                AnnotationLoader.UNSUPPORTED, ConstantLoader.UNSUPPORTED, packageFragmentProvider,
                nameResolver
        );
        members = new DeserializedPackageMemberScope(this, loadPackage(), context);
    }

    @NotNull
    private ProtoBuf.Package loadPackage() {
        String packageFilePath = BuiltInsSerializationUtil.getPackageFilePath(getFqName());
        InputStream stream = getStream(packageFilePath);
        try {
            return ProtoBuf.Package.parseFrom(stream);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return members;
    }

    @NotNull
    public PackageFragmentProvider getProvider() {
        return packageFragmentProvider;
    }

    @NotNull
    private static InputStream getStream(@NotNull String path) {
        InputStream stream = getStreamNullable(path);
        if (stream == null) {
            throw new IllegalStateException("Resource not found in classpath: " + path);
        }
        return stream;
    }

    @Nullable
    private static InputStream getStreamNullable(@NotNull String path) {
        return KotlinBuiltIns.class.getClassLoader().getResourceAsStream(path);
    }

    private class BuiltinsPackageFragmentProvider implements PackageFragmentProvider {
        @NotNull
        @Override
        public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
            if (KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(fqName)) {
                return Collections.<PackageFragmentDescriptor>singletonList(BuiltinsPackageFragment.this);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
            if (fqName.isRoot()) {
                return Collections.singleton(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
            }
            return Collections.emptyList();
        }
    }

    private class BuiltInsDescriptorFinder extends AbstractDescriptorFinder {
        private final NotNullLazyValue<Collection<Name>> classNames;

        public BuiltInsDescriptorFinder(@NotNull StorageManager storageManager) {
            // TODO: support annotations
            super(storageManager, AnnotationLoader.UNSUPPORTED, ConstantLoader.UNSUPPORTED, packageFragmentProvider);

            classNames = storageManager.createLazyValue(new Function0<Collection<Name>>() {
                @Override
                @NotNull
                public Collection<Name> invoke() {
                    InputStream in = getStream(BuiltInsSerializationUtil.getClassNamesFilePath(getFqName()));

                    try {
                        DataInputStream data = new DataInputStream(in);
                        try {
                            int size = data.readInt();
                            List<Name> result = new ArrayList<Name>(size);
                            for (int i = 0; i < size; i++) {
                                result.add(nameResolver.getName(data.readInt()));
                            }
                            return result;
                        }
                        finally {
                            data.close();
                        }
                    }
                    catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
        }

        @Nullable
        @Override
        protected ClassData getClassData(@NotNull ClassId classId) {
            InputStream stream = getStreamNullable(BuiltInsSerializationUtil.getClassMetadataPath(classId));
            if (stream == null) {
                return null;
            }

            try {
                ProtoBuf.Class classProto = ProtoBuf.Class.parseFrom(stream);

                Name expectedShortName = classId.getRelativeClassName().shortName();
                Name actualShortName = nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName();
                if (!actualShortName.isSpecial() && !actualShortName.equals(expectedShortName)) {
                    // Workaround for case-insensitive file systems,
                    // otherwise we'd find "Collection" for "collection" etc
                    return null;
                }

                return new ClassData(nameResolver, classProto);
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            return packageName.equals(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) ? classNames.invoke() : Collections.<Name>emptyList();
        }
    }
}

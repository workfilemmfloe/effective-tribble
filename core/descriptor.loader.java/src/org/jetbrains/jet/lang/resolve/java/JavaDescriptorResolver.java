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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.lazy.GlobalJavaResolverContext;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaClassResolver;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedDescriptorResolver;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinClassFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.storage.StorageManager;

import javax.inject.Inject;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class JavaDescriptorResolver implements DependencyClassByQualifiedNameResolver {
    private final boolean LAZY;

    {
        LAZY = true;
    }

    public static final Name JAVA_ROOT = Name.special("<java_root>");

    private StorageManager storageManager;
    private JavaClassResolver classResolver;
    private JavaPackageFragmentProvider packageFragmentProvider;
    private JavaClassFinder javaClassFinder;
    private ExternalAnnotationResolver externalAnnotationResolver;
    private LazyJavaPackageFragmentProvider lazyJavaPackageFragmentProvider;
    private ExternalSignatureResolver externalSignatureResolver;
    private ErrorReporter errorReporter;
    private MethodSignatureChecker signatureChecker;
    private JavaResolverCache javaResolverCache;
    private DeserializedDescriptorResolver deserializedDescriptorResolver;
    private KotlinClassFinder kotlinClassFinder;
    private ModuleDescriptor module;

    @Inject
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setPackageFragmentProvider(JavaPackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @Inject
    public void setJavaClassFinder(JavaClassFinder javaClassFinder) {
        this.javaClassFinder = javaClassFinder;
    }

    @Inject
    public void setExternalAnnotationResolver(ExternalAnnotationResolver externalAnnotationResolver) {
        this.externalAnnotationResolver = externalAnnotationResolver;
    }

    @Inject
    public void setExternalSignatureResolver(ExternalSignatureResolver externalSignatureResolver) {
        this.externalSignatureResolver = externalSignatureResolver;
    }

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Inject
    public void setSignatureChecker(MethodSignatureChecker signatureChecker) {
        this.signatureChecker = signatureChecker;
    }

    @Inject
    public void setJavaResolverCache(JavaResolverCache javaResolverCache) {
        this.javaResolverCache = javaResolverCache;
    }

    @Inject
    public void setDeserializedDescriptorResolver(DeserializedDescriptorResolver deserializedDescriptorResolver) {
        this.deserializedDescriptorResolver = deserializedDescriptorResolver;
    }

    @Inject
    public void setKotlinClassFinder(KotlinClassFinder kotlinClassFinder) {
        this.kotlinClassFinder = kotlinClassFinder;
    }

    @Inject
    public void setModule(@NotNull ModuleDescriptor module) {
        this.module = module;
    }

    @NotNull
    public ModuleDescriptor getModule() {
        return module;
    }

    private LazyJavaPackageFragmentProvider getLazyJavaPackageFragmentProvider() {
        if (lazyJavaPackageFragmentProvider == null) {
            lazyJavaPackageFragmentProvider = new LazyJavaPackageFragmentProvider(
                    new GlobalJavaResolverContext(
                            storageManager,
                            javaClassFinder,
                            kotlinClassFinder,
                            deserializedDescriptorResolver,
                            new LazyJavaClassResolver() {
                                @Override
                                public ClassDescriptor resolveClass(JavaClass aClass) {
                                    FqName fqName = aClass.getFqName();
                                    if (fqName != null) {
                                        return resolveClassByFqName(fqName);
                                    }

                                    return null;
                                }

                                @Override
                                public ClassDescriptor resolveClassByFqName(FqName fqName) {
                                    return javaResolverCache.getClassResolvedFromSource(fqName);
                                }
                            },
                            externalAnnotationResolver,
                            externalSignatureResolver,
                            errorReporter,
                            signatureChecker,
                            javaResolverCache,
                            this
                    ),
                    module
            );
        }
        return lazyJavaPackageFragmentProvider;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        if (LAZY) {
            return getLazyJavaPackageFragmentProvider().getClass(qualifiedName);
        }
        return classResolver.resolveClass(qualifiedName, searchRule);
    }

    @Override
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return resolveClass(qualifiedName, IGNORE_KOTLIN_SOURCES);
    }

    @NotNull
    public JavaPackageFragmentProvider getPackageFragmentProvider() {
        if (LAZY) {
            return getLazyJavaPackageFragmentProvider();
        }
        return packageFragmentProvider;
    }

    @Nullable
    public PackageFragmentDescriptor getPackageFragment(@NotNull FqName fqName) {
        if (LAZY) {
            return getLazyJavaPackageFragmentProvider().getPackageFragment(fqName);
        }
        return packageFragmentProvider.getPackageFragment(fqName);
    }
}

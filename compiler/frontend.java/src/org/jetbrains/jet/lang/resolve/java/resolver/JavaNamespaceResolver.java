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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Maps;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ModuleDescriptorProvider;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.scope.JavaBaseScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScopeWithoutMembers;
import org.jetbrains.jet.lang.resolve.java.scope.JavaScopeForKotlinNamespace;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProviderFactory.*;

public final class JavaNamespaceResolver {

    @NotNull
    private final Map<FqName, Map<ModuleDescriptor, NamespaceDescriptor>> cache = Maps.newHashMap();

    private PsiClassFinder psiClassFinder;
    private BindingTrace trace;
    private JavaSemanticServices javaSemanticServices;
    private ModuleDescriptorProvider moduleDescriptorProvider;

    public JavaNamespaceResolver() {
    }

    @Inject
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setJavaSemanticServices(JavaSemanticServices javaSemanticServices) {
        this.javaSemanticServices = javaSemanticServices;
    }

    @Inject
    public void setModuleDescriptorProvider(ModuleDescriptorProvider moduleDescriptorProvider) {
        this.moduleDescriptorProvider = moduleDescriptorProvider;
    }


    @Nullable
    public NamespaceDescriptor resolveNamespace(
            @NotNull FqName qualifiedName,
            @NotNull ModuleDescriptor parentModule
    ) {
        Map<ModuleDescriptor, NamespaceDescriptor> map = resolveNamespace(qualifiedName);
        return map.get(parentModule);
    }

    @NotNull
    public Map<ModuleDescriptor, NamespaceDescriptor> resolveNamespace(
            @NotNull FqName fqName
    ) {
        Map<ModuleDescriptor, NamespaceDescriptor> cachedResult = cache.get(fqName);
        if (cachedResult != null) {
            return cachedResult;
        }
        Map<ModuleDescriptor, NamespaceDescriptor> result = new HashMap<ModuleDescriptor, NamespaceDescriptor>();
        for (ModuleDescriptor moduleDescriptor : moduleDescriptorProvider.getAllModules()) {
            NamespaceDescriptor resolvedNamespace = doResolveNamespace(fqName, moduleDescriptor);
            if (resolvedNamespace != null) {
                result.put(moduleDescriptor, resolvedNamespace);
            }
        }
        cache.put(fqName, result);
        return result;
    }

    @Nullable
    private NamespaceDescriptor doResolveNamespace(
            @NotNull FqName fqName,
            @NotNull ModuleDescriptor parentModule
    ) {
        NamespaceDescriptorParent parentNs = resolveParentNamespace(fqName, parentModule);
        if (parentNs == null) {
            return null;
        }

        JavaNamespaceDescriptor javaNamespaceDescriptor = new JavaNamespaceDescriptor(
                parentNs,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                fqName
        );

        JavaBaseScope newScope = createNamespaceScope(fqName, javaNamespaceDescriptor, parentModule);
        if (newScope == null) {
            return null;
        }

        trace.record(BindingContext.NAMESPACE_FQNAME_TO_PSI, fqName, newScope.getPsiElement());

        javaNamespaceDescriptor.setMemberScope(newScope);

        return javaNamespaceDescriptor;
    }

    @Nullable
    private NamespaceDescriptorParent resolveParentNamespace(@NotNull FqName fqName, @NotNull ModuleDescriptor parentModule) {
        if (fqName.isRoot()) {
            return parentModule;
        }
        else {
            return resolveNamespace(fqName.parent(), parentModule);
        }
    }

    @Nullable
    private JavaBaseScope createNamespaceScope(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor,
            @NotNull ModuleDescriptor parentModule
    ) {
        PsiPackage psiPackage = psiClassFinder.findPsiPackage(fqName);
        if (psiPackage != null) {
            PsiClass psiClass = getPsiClassForJavaPackageScope(fqName);
            trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.PROPER);
            GlobalSearchScope searchScopeForModule = moduleDescriptorProvider.getSearchScopeForModule(parentModule);
            if (psiClass != null) {
                return new JavaScopeForKotlinNamespace(namespaceDescriptor,
                                                       createDeclarationForKotlinNamespace(psiPackage, psiClass, searchScopeForModule),
                                                       fqName, parentModule, javaSemanticServices.getDescriptorResolver());
            }
            return new JavaPackageScopeWithoutMembers(namespaceDescriptor,
                                                      createDeclarationProviderForNamespaceWithoutMembers(psiPackage, searchScopeForModule),
                                                      fqName, parentModule, javaSemanticServices.getDescriptorResolver());
        }

        PsiClass psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
        if (psiClass == null) {
            return null;
        }
        if (psiClass.isEnum()) {
            // NOTE: we don't want to create namespace for enum classes because we put
            // static members of enum class into class object descriptor
            return null;
        }
        trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.CLASS_STATICS);
        return new JavaClassStaticMembersScope(namespaceDescriptor,
                                               createDeclarationProviderForClassStaticMembers(psiClass),
                                               fqName, javaSemanticServices.getDescriptorResolver());
    }

    @Nullable
    private PsiClass getPsiClassForJavaPackageScope(@NotNull FqName packageFQN) {
        return psiClassFinder.findPsiClass(packageFQN.child(Name.identifier(JvmAbi.PACKAGE_CLASS)),
                                           PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
    }
}
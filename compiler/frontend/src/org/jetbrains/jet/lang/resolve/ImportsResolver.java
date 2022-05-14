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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubPackagesScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode;

public class ImportsResolver {
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ModuleSourcesManager moduleSourcesManager;
    @NotNull
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    @NotNull
    private BindingTrace trace;
    @NotNull
    private JetImportsFactory importsFactory;

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setModuleSourcesManager(@NotNull ModuleSourcesManager moduleSourcesManager) {
        this.moduleSourcesManager = moduleSourcesManager;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setQualifiedExpressionResolver(@NotNull QualifiedExpressionResolver qualifiedExpressionResolver) {
        this.qualifiedExpressionResolver = qualifiedExpressionResolver;
    }

    @Inject
    public void setImportsFactory(@NotNull JetImportsFactory importsFactory) {
        this.importsFactory = importsFactory;
    }

    public void processTypeImports() {
        processImports(LookupMode.ONLY_CLASSES);
    }

    public void processMembersImports() {
        processImports(LookupMode.EVERYTHING);
    }

    private void processImports(@NotNull LookupMode lookupMode) {
        for (JetFile file : context.getPackageFragmentDescriptors().keySet()) {
            WritableScope namespaceScope = context.getFileScopes().get(file);
            SubModuleDescriptor subModule = moduleSourcesManager.getSubModuleForFile(file);
            processImportsInFile(subModule, lookupMode, namespaceScope, Lists.newArrayList(file.getImportDirectives())
            );
        }
        for (JetScript script : context.getScripts().keySet()) {
            WritableScope scriptScope = context.getScriptScopes().get(script);
            SubModuleDescriptor subModule = moduleSourcesManager.getSubModuleForFile(script.getContainingFile());
            processImportsInFile(subModule, lookupMode, scriptScope, script.getImportDirectives());
        }
    }

    private void processImportsInFile(
            @NotNull SubModuleDescriptor subModule,
            @NotNull LookupMode lookupMode,
            WritableScope scope,
            List<JetImportDirective> directives
    ) {
        processImportsInFile(lookupMode, scope, directives, subModule, trace, qualifiedExpressionResolver, importsFactory);
    }

    public static void processImportsInFile(
            LookupMode lookupMode,
            @NotNull WritableScope namespaceScope,
            @NotNull List<JetImportDirective> importDirectives,
            @NotNull SubModuleDescriptor subModule,
            @NotNull BindingTrace trace,
            @NotNull QualifiedExpressionResolver qualifiedExpressionResolver,
            @NotNull JetImportsFactory importsFactory
    ) {
        Importer.DelayedImporter delayedImporter = new Importer.DelayedImporter(namespaceScope);
        if (lookupMode == LookupMode.EVERYTHING) {
            namespaceScope.clearImports();
        }

        PackageViewDescriptor rootPackage = DescriptorUtils.getRootPackage(subModule);
        JetScope rootScope = rootPackage.getMemberScope();

        // We should be able to resolve top-level packages from anywhere
        namespaceScope.importScope(new SubPackagesScope(rootPackage));

        PlatformToKotlinClassMap platformToKotlinClassMap = subModule.getContainingDeclaration().getPlatformToKotlinClassMap();
        for (ImportPath defaultImportPath : subModule.getDefaultImports()) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                    trace, "transient trace to resolve default imports"); //not to trace errors of default imports

            JetImportDirective defaultImportDirective = importsFactory.createImportDirective(defaultImportPath);
            qualifiedExpressionResolver.processImportReference(defaultImportDirective, rootScope, namespaceScope, delayedImporter,
                                                               temporaryTrace, platformToKotlinClassMap, lookupMode);
        }

        Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives = Maps.newHashMap();

        for (JetImportDirective importDirective : importDirectives) {
            Collection<? extends DeclarationDescriptor> descriptors =
                qualifiedExpressionResolver.processImportReference(importDirective, rootScope, namespaceScope, delayedImporter,
                                                                   trace, platformToKotlinClassMap, lookupMode);
            if (descriptors.size() == 1) {
                resolvedDirectives.put(importDirective, descriptors.iterator().next());
            }
            for (DeclarationDescriptor descriptor : descriptors) {
                JetExpression importedReference = importDirective.getImportedReference();
                if (lookupMode == LookupMode.ONLY_CLASSES || importedReference == null) continue;
                reportPlatformClassMappedToKotlin(platformToKotlinClassMap, trace, importedReference, descriptor);
            }
        }
        delayedImporter.processImports();

        if (lookupMode == LookupMode.EVERYTHING) {
            for (JetImportDirective importDirective : importDirectives) {
                reportUselessImport(importDirective, namespaceScope, resolvedDirectives, trace);
            }
        }
    }

    public static void reportPlatformClassMappedToKotlin(
            @NotNull PlatformToKotlinClassMap classMap,
            @NotNull BindingTrace trace,
            @NotNull JetElement element,
            @NotNull DeclarationDescriptor descriptor
    ) {
        if (!(descriptor instanceof ClassDescriptor)) return;

        Collection<ClassDescriptor> kotlinAnalogsForClass = classMap.mapPlatformClass((ClassDescriptor) descriptor);
        if (!kotlinAnalogsForClass.isEmpty()) {
            trace.report(PLATFORM_CLASS_MAPPED_TO_KOTLIN.on(element, kotlinAnalogsForClass));
        }
    }

    private static void reportUselessImport(
        @NotNull JetImportDirective importDirective,
        @NotNull WritableScope namespaceScope,
        @NotNull Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives,
        @NotNull BindingTrace trace
    ) {

        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null || !resolvedDirectives.containsKey(importDirective)) {
            return;
        }
        Name aliasName = JetPsiUtil.getAliasName(importDirective);
        if (aliasName == null) {
            return;
        }

        DeclarationDescriptor wasResolved = resolvedDirectives.get(importDirective);
        DeclarationDescriptor isResolved = null;
        if (wasResolved instanceof ClassDescriptor) {
            isResolved = namespaceScope.getClassifier(aliasName);
        }
        else if (wasResolved instanceof VariableDescriptor) {
            isResolved = namespaceScope.getLocalVariable(aliasName);
        }
        else if (wasResolved instanceof PackageViewDescriptor) {
            isResolved = namespaceScope.getPackage(aliasName);
        }
        if (isResolved != null && isResolved != wasResolved) {
            trace.report(USELESS_HIDDEN_IMPORT.on(importedReference));
        }
        if (!importDirective.isAllUnder() &&
            importedReference instanceof JetSimpleNameExpression &&
            importDirective.getAliasName() == null) {
            trace.report(USELESS_SIMPLE_IMPORT.on(importedReference));
        }
    }
}

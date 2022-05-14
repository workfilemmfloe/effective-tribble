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

package org.jetbrains.kotlin.resolve.lazy.descriptors;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import kotlin.CollectionsKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.resolve.lazy.data.JetClassInfoUtil;
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.kotlin.resolve.lazy.data.JetClassOrObjectInfo;
import org.jetbrains.kotlin.resolve.lazy.data.JetObjectInfo;
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.NullableLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.*;

import static kotlin.CollectionsKt.firstOrNull;
import static org.jetbrains.kotlin.diagnostics.Errors.CYCLIC_INHERITANCE_HIERARCHY;
import static org.jetbrains.kotlin.diagnostics.Errors.TYPE_PARAMETERS_IN_ENUM;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.*;
import static org.jetbrains.kotlin.resolve.source.SourcePackage.toSourceElement;

public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes, LazyEntity {
    private static final Predicate<JetType> VALID_SUPERTYPE = new Predicate<JetType>() {
        @Override
        public boolean apply(JetType type) {
            assert !type.isError() : "Error types must be filtered out in DescriptorResolver";
            return TypeUtils.getClassDescriptor(type) != null;
        }
    };
    private final LazyClassContext c;

    private final ClassMemberDeclarationProvider declarationProvider;

    private final LazyClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final NotNullLazyValue<ClassKind> kind;
    private final NotNullLazyValue<Boolean> isInner;

    private final Annotations annotations;
    private final Annotations danglingAnnotations;
    private final NullableLazyValue<LazyClassDescriptor> companionObjectDescriptor;
    private final MemoizedFunctionToNotNull<JetObjectDeclaration, ClassDescriptor> extraCompanionObjectDescriptors;

    private final LazyClassMemberScope unsubstitutedMemberScope;
    private final JetScope staticScope;

    private final NullableLazyValue<Void> forceResolveAllContents;
    private final boolean isCompanionObject;

    private final ClassResolutionScopesSupport resolutionScopesSupport;

    public LazyClassDescriptor(
            @NotNull LazyClassContext c,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull JetClassLikeInfo classLikeInfo
    ) {
        super(c.getStorageManager(), containingDeclaration, name,
              toSourceElement(classLikeInfo.getCorrespondingClassOrObject())
        );
        this.c = c;

        JetClassOrObject classOrObject = classLikeInfo.getCorrespondingClassOrObject();
        if (classOrObject != null) {
            this.c.getTrace().record(BindingContext.CLASS, classOrObject, this);
        }
        this.c.getTrace().record(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(this), this);

        this.declarationProvider = c.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfo);

        this.unsubstitutedMemberScope = createMemberScope(c, this.declarationProvider);
        this.staticScope = new StaticScopeForKotlinClass(this);

        this.typeConstructor = new LazyClassTypeConstructor();

        final ClassKind syntaxKind = classLikeInfo.getClassKind();
        this.isCompanionObject = classLikeInfo instanceof JetObjectInfo && ((JetObjectInfo) classLikeInfo).isCompanionObject();

        final JetModifierList modifierList = classLikeInfo.getModifierList();
        if (syntaxKind.isSingleton()) {
            this.modality = Modality.FINAL;
        }
        else {
            Modality defaultModality = syntaxKind == ClassKind.INTERFACE ? Modality.ABSTRACT : Modality.FINAL;
            this.modality = resolveModalityFromModifiers(modifierList, defaultModality);
        }

        boolean isLocal = classOrObject != null && JetPsiUtil.isLocal(classOrObject);
        Visibility defaultVisibility;
        if (syntaxKind == ClassKind.ENUM_ENTRY || (syntaxKind == ClassKind.OBJECT && isCompanionObject)) {
            defaultVisibility = Visibilities.PUBLIC;
        }
        else {
            defaultVisibility = Visibilities.DEFAULT_VISIBILITY;
        }
        this.visibility = isLocal ? Visibilities.LOCAL : resolveVisibilityFromModifiers(modifierList, defaultVisibility);

        StorageManager storageManager = c.getStorageManager();
        final ClassDescriptor descriptor = this;

        this.isInner = storageManager.createLazyValue(new Function0<Boolean>() {
            @Override
            public Boolean invoke() {
                return isInnerClass(modifierList) && !ModifiersChecker.isIllegalInner(descriptor);
            }
        });

        this.kind = storageManager.createLazyValue(new Function0<ClassKind>() {
            @Override
            public ClassKind invoke() {
                return (syntaxKind == ClassKind.CLASS && KotlinBuiltIns.isAnnotation(descriptor)) ? ClassKind.ANNOTATION_CLASS : syntaxKind;
            }
        });

        if (modifierList != null) {
            LazyAnnotations classAnnotations = new LazyAnnotations(
                    new LazyAnnotationsContext(
                            c.getAnnotationResolver(),
                            storageManager,
                            c.getTrace()
                    ) {
                        @NotNull
                        @Override
                        public LexicalScope getScope() {
                            return getOuterScope();
                        }
                    },
                    modifierList.getAnnotationEntries()
            );
            this.annotations = c.getAnnotationResolver().resolveAndAppendAnnotationsFromModifiers(
                    classAnnotations, modifierList
            );
        }
        else {
            this.annotations = Annotations.Companion.getEMPTY();
        }

        List<JetAnnotationEntry> jetDanglingAnnotations = classLikeInfo.getDanglingAnnotations();
        if (jetDanglingAnnotations.isEmpty()) {
            this.danglingAnnotations = Annotations.Companion.getEMPTY();
        }
        else {
            this.danglingAnnotations = new LazyAnnotations(
                    new LazyAnnotationsContext(
                            c.getAnnotationResolver(),
                            storageManager,
                            c.getTrace()
                    ) {
                        @NotNull
                        @Override
                        public LexicalScope getScope() {
                            return getScopeForMemberDeclarationResolution();
                        }
                    },
                    jetDanglingAnnotations
            );
        }

        this.companionObjectDescriptor = storageManager.createNullableLazyValue(new Function0<LazyClassDescriptor>() {
            @Override
            public LazyClassDescriptor invoke() {
                return computeCompanionObjectDescriptor(getCompanionObjectIfAllowed());
            }
        });
        this.extraCompanionObjectDescriptors = storageManager.createMemoizedFunction(new Function1<JetObjectDeclaration, ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke(JetObjectDeclaration companionObject) {
                return computeCompanionObjectDescriptor(companionObject);
            }
        });
        this.forceResolveAllContents = storageManager.createRecursionTolerantNullableLazyValue(new Function0<Void>() {
            @Override
            public Void invoke() {
                doForceResolveAllContents();
                return null;
            }
        }, null);

        this.resolutionScopesSupport = new ClassResolutionScopesSupport(this, storageManager, new Function0<LexicalScope>() {
            @Override
            public LexicalScope invoke() {
                return getOuterScope();
            }
        }, classLikeInfo.getPrimaryConstructorParameters());
    }

    // NOTE: Called from constructor!
    @NotNull
    protected LazyClassMemberScope createMemberScope(
            @NotNull LazyClassContext c,
            @NotNull ClassMemberDeclarationProvider declarationProvider
    ) {
        return new LazyClassMemberScope(c, declarationProvider, this, c.getTrace());
    }

    @NotNull
    @Override
    public JetScope getUnsubstitutedMemberScope() {
        return unsubstitutedMemberScope;
    }

    @NotNull
    protected LexicalScope getOuterScope() {
        return c.getDeclarationScopeProvider().getResolutionScopeForDeclaration(declarationProvider.getOwnerInfo().getScopeAnchor());
    }

    @Override
    @NotNull
    public LexicalScope getScopeForClassHeaderResolution() {
        return resolutionScopesSupport.getScopeForClassHeaderResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForMemberDeclarationResolution() {
        return resolutionScopesSupport.getScopeForMemberDeclarationResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForStaticMemberDeclarationResolution() {
        return resolutionScopesSupport.getScopeForStaticMemberDeclarationResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForInitializerResolution() {
        return resolutionScopesSupport.getScopeForInitializerResolution().invoke();
    }

    @NotNull
    @Override
    public Collection<CallableMemberDescriptor> getDeclaredCallableMembers() {
        //noinspection unchecked
        return (Collection) CollectionsKt.filter(
                unsubstitutedMemberScope.getAllDescriptors(),
                new Function1<DeclarationDescriptor, Boolean>() {
                    @Override
                    public Boolean invoke(DeclarationDescriptor descriptor) {
                        return descriptor instanceof CallableMemberDescriptor
                               && ((CallableMemberDescriptor) descriptor).getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
                    }
                }
        );
    }

    @NotNull
    @Override
    public JetScope getStaticScope() {
        return staticScope;
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        return unsubstitutedMemberScope.getConstructors();
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return unsubstitutedMemberScope.getPrimaryConstructor();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public LazyClassDescriptor getCompanionObjectDescriptor() {
        return companionObjectDescriptor.invoke();
    }

    @NotNull
    @ReadOnly
    public List<ClassDescriptor> getDescriptorsForExtraCompanionObjects() {
        final JetObjectDeclaration allowedCompanionObject = getCompanionObjectIfAllowed();

        return CollectionsKt.map(
                CollectionsKt.filter(
                        declarationProvider.getOwnerInfo().getCompanionObjects(),
                        new Function1<JetObjectDeclaration, Boolean>() {
                            @Override
                            public Boolean invoke(JetObjectDeclaration companionObject) {
                                return companionObject != allowedCompanionObject;
                            }
                        }
                ),
                new Function1<JetObjectDeclaration, ClassDescriptor>() {
                    @Override
                    public ClassDescriptor invoke(JetObjectDeclaration companionObject) {
                        return extraCompanionObjectDescriptors.invoke(companionObject);
                    }
                }
        );
    }

    @Nullable
    private LazyClassDescriptor computeCompanionObjectDescriptor(@Nullable JetObjectDeclaration companionObject) {
        JetClassLikeInfo companionObjectInfo = getCompanionObjectInfo(companionObject);
        if (!(companionObjectInfo instanceof JetClassOrObjectInfo)) {
            return null;
        }
        Name name = ((JetClassOrObjectInfo) companionObjectInfo).getName();
        assert name != null;
        getUnsubstitutedMemberScope().getClassifier(name, NoLookupLocation.UNSORTED);
        ClassDescriptor companionObjectDescriptor = c.getTrace().get(BindingContext.CLASS, companionObject);
        if (companionObjectDescriptor instanceof LazyClassDescriptor) {
            assert DescriptorUtils.isCompanionObject(companionObjectDescriptor) : "Not a companion object: " + companionObjectDescriptor;
            return (LazyClassDescriptor) companionObjectDescriptor;
        }
        else {
            return null;
        }
    }

    @Nullable
    private static JetClassLikeInfo getCompanionObjectInfo(@Nullable JetObjectDeclaration companionObject) {
        if (companionObject != null) {
            return JetClassInfoUtil.createClassLikeInfo(companionObject);
        }

        return null;
    }

    @Nullable
    private JetObjectDeclaration getCompanionObjectIfAllowed() {
        JetObjectDeclaration companionObject = firstOrNull(declarationProvider.getOwnerInfo().getCompanionObjects());
        return (companionObject != null && isCompanionObjectAllowed()) ? companionObject : null;
    }

    private boolean isCompanionObjectAllowed() {
        return !(getKind().isSingleton() || isInner() || DescriptorUtils.isLocal(this));
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind.invoke();
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public boolean isInner() {
        return isInner.invoke();
    }

    @Override
    public boolean isCompanionObject() {
        return isCompanionObject;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return annotations;
    }

    @NotNull
    public Annotations getDanglingAnnotations() {
        return danglingAnnotations;
    }

    @Override
    public String toString() {
        // not using descriptor render to preserve laziness
        return "lazy class " + getName().toString();
    }

    @Override
    public void forceResolveAllContents() {
        forceResolveAllContents.invoke();
    }

    private void doForceResolveAllContents() {
        resolveMemberHeaders();
        ClassDescriptor companionObjectDescriptor = getCompanionObjectDescriptor();
        if (companionObjectDescriptor != null) {
            ForceResolveUtil.forceResolveAllContents(companionObjectDescriptor);
        }

        ForceResolveUtil.forceResolveAllContents(getConstructors());
        ForceResolveUtil.forceResolveAllContents(getDescriptorsForExtraCompanionObjects());
        ForceResolveUtil.forceResolveAllContents(getUnsubstitutedMemberScope());
        ForceResolveUtil.forceResolveAllContents(getTypeConstructor());
    }

    // Note: headers of member classes' members are not resolved
    public void resolveMemberHeaders() {
        ForceResolveUtil.forceResolveAllContents(getAnnotations());
        ForceResolveUtil.forceResolveAllContents(getDanglingAnnotations());

        getCompanionObjectDescriptor();

        getDescriptorsForExtraCompanionObjects();

        getConstructors();
        getContainingDeclaration();
        getThisAsReceiverParameter();
        getKind();
        getModality();
        getName();
        getOriginal();
        getScopeForClassHeaderResolution();
        getScopeForMemberDeclarationResolution();
        getUnsubstitutedMemberScope().getAllDescriptors();
        getScopeForInitializerResolution();
        getUnsubstitutedInnerClassesScope();
        getTypeConstructor().getSupertypes();
        for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
            typeParameterDescriptor.getUpperBounds();
            typeParameterDescriptor.getLowerBounds();
        }
        getUnsubstitutedPrimaryConstructor();
        getVisibility();
    }

    private static class Supertypes {
        @Mutable
        public final Collection<JetType> trueSupertypes;
        @Mutable
        public final Collection<JetType> cyclicSupertypes;

        private Supertypes(@Mutable @NotNull Collection<JetType> trueSupertypes) {
            this(trueSupertypes, new ArrayList<JetType>(0));
        }

        private Supertypes(@Mutable @NotNull Collection<JetType> trueSupertypes, @Mutable @NotNull Collection<JetType> cyclicSupertypes) {
            this.trueSupertypes = trueSupertypes;
            this.cyclicSupertypes = cyclicSupertypes;
        }

        @NotNull
        public Collection<JetType> getAllSupertypes() {
            return CollectionsKt.plus(trueSupertypes, cyclicSupertypes);
        }
    }

    private class LazyClassTypeConstructor extends AbstractClassTypeConstructor implements LazyEntity {
        private final NotNullLazyValue<Supertypes> supertypes = c.getStorageManager().createLazyValueWithPostCompute(
                new Function0<Supertypes>() {
                    @Override
                    public Supertypes invoke() {
                        if (KotlinBuiltIns.isSpecialClassWithNoSupertypes(LazyClassDescriptor.this)) {
                            return new Supertypes(Collections.<JetType>emptyList());
                        }

                        JetClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
                        if (classOrObject == null) {
                            return new Supertypes(Collections.singleton(c.getModuleDescriptor().getBuiltIns().getAnyType()));
                        }

                        List<JetType> allSupertypes = c.getDescriptorResolver()
                                .resolveSupertypes(getScopeForClassHeaderResolution(), LazyClassDescriptor.this, classOrObject,
                                                   c.getTrace());

                        return new Supertypes(Lists.newArrayList(Collections2.filter(allSupertypes, VALID_SUPERTYPE)));
                    }
                },
                new Function1<Boolean, Supertypes>() {
                    @Override
                    public Supertypes invoke(Boolean firstTime) {
                        return new Supertypes(Collections.<JetType>emptyList());
                    }
                },
                new Function1<Supertypes, Unit>() {
                    @Override
                    public Unit invoke(@NotNull Supertypes supertypes) {
                        findAndDisconnectLoopsInTypeHierarchy(supertypes);
                        return Unit.INSTANCE$;
                    }
                }
        );

        private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters = c.getStorageManager().createLazyValue(new Function0<List<TypeParameterDescriptor>>() {
            @Override
            public List<TypeParameterDescriptor> invoke() {
                JetClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
                JetTypeParameterList typeParameterList = classInfo.getTypeParameterList();
                if (typeParameterList == null) return Collections.emptyList();

                if (classInfo.getClassKind() == ClassKind.ENUM_CLASS) {
                    c.getTrace().report(TYPE_PARAMETERS_IN_ENUM.on(typeParameterList));
                }

                List<JetTypeParameter> typeParameters = typeParameterList.getParameters();
                List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>(typeParameters.size());
                for (int i = 0; i < typeParameters.size(); i++) {
                    parameters.add(new LazyTypeParameterDescriptor(c, LazyClassDescriptor.this, typeParameters.get(i), i));
                }

                return parameters;
            }
        });

        private final NullableLazyValue<Void> forceResolveAllContents =
                c.getStorageManager().createRecursionTolerantNullableLazyValue(new Function0<Void>() {
                    @Override
                    public Void invoke() {
                        doForceResolveAllContents();
                        return null;
                    }
                }, null);

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return parameters.invoke();
        }

        @NotNull
        @Override
        public Collection<JetType> getSupertypes() {
            return supertypes.invoke().trueSupertypes;
        }

        private void findAndDisconnectLoopsInTypeHierarchy(Supertypes supertypes) {
            for (Iterator<JetType> iterator = supertypes.trueSupertypes.iterator(); iterator.hasNext(); ) {
                JetType supertype = iterator.next();
                if (isReachable(supertype.getConstructor(), this, new HashSet<TypeConstructor>())) {
                    iterator.remove();
                    supertypes.cyclicSupertypes.add(supertype);

                    ClassifierDescriptor supertypeDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                    if (supertypeDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor superclass = (ClassDescriptor) supertypeDescriptor;
                        reportCyclicInheritanceHierarchyError(c.getTrace(), LazyClassDescriptor.this, superclass);
                    }
                }
            }
        }

        private void reportCyclicInheritanceHierarchyError(
                @NotNull BindingTrace trace,
                @NotNull ClassDescriptor classDescriptor,
                @NotNull ClassDescriptor superclass
        ) {
            PsiElement psiElement = DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor);

            PsiElement elementToMark = null;
            if (psiElement instanceof JetClassOrObject) {
                JetClassOrObject classOrObject = (JetClassOrObject) psiElement;
                for (JetDelegationSpecifier delegationSpecifier : classOrObject.getDelegationSpecifiers()) {
                    JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                    if (typeReference == null) continue;
                    JetType supertype = trace.get(TYPE, typeReference);
                    if (supertype != null && supertype.getConstructor() == superclass.getTypeConstructor()) {
                        elementToMark = typeReference;
                    }
                }
            }
            if (elementToMark == null && psiElement instanceof PsiNameIdentifierOwner) {
                PsiNameIdentifierOwner namedElement = (PsiNameIdentifierOwner) psiElement;
                PsiElement nameIdentifier = namedElement.getNameIdentifier();
                if (nameIdentifier != null) {
                    elementToMark = nameIdentifier;
                }
            }
            if (elementToMark != null) {
                trace.report(CYCLIC_INHERITANCE_HIERARCHY.on(elementToMark));
            }
        }

        private boolean isReachable(TypeConstructor from, TypeConstructor to, Set<TypeConstructor> visited) {
            if (!visited.add(from)) return false;
            for (JetType supertype : getNeighbors(from)) {
                TypeConstructor supertypeConstructor = supertype.getConstructor();
                if (supertypeConstructor == to) {
                    return true;
                }
                if (isReachable(supertypeConstructor, to, visited)) {
                    return true;
                }
            }
            return false;
        }

        private Collection<JetType> getNeighbors(TypeConstructor from) {
            // Supertypes + type for container
            Collection<JetType> neighbours = new ArrayList<JetType>(
                    from instanceof LazyClassTypeConstructor
                             ? ((LazyClassTypeConstructor) from).supertypes.invoke().getAllSupertypes()
                             : from.getSupertypes()
            );

            ClassifierDescriptor fromDescriptor = from.getDeclarationDescriptor();
            if (fromDescriptor != null) {
                DeclarationDescriptor container = fromDescriptor.getContainingDeclaration();
                if (container instanceof ClassDescriptor) {
                    neighbours.add(((ClassDescriptor) container).getDefaultType());
                }
            }
            return neighbours;
        }

        @Override
        public boolean isFinal() {
            return !getModality().isOverridable();
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @Override
        @NotNull
        public ClassifierDescriptor getDeclarationDescriptor() {
            return LazyClassDescriptor.this;
        }

        @NotNull
        @Override
        public Annotations getAnnotations() {
            return Annotations.Companion.getEMPTY(); // TODO
        }

        @Override
        public String toString() {
            return LazyClassDescriptor.this.getName().toString();
        }

        @Override
        public void forceResolveAllContents() {
            forceResolveAllContents.invoke();
        }

        private void doForceResolveAllContents() {
            ForceResolveUtil.forceResolveAllContents(getAnnotations());
            ForceResolveUtil.forceResolveAllContents(getSupertypes());
            ForceResolveUtil.forceResolveAllContents(getParameters());
        }
    }
}

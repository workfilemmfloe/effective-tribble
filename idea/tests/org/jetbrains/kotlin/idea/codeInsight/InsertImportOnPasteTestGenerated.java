/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@RunWith(JUnit3RunnerWithInners.class)
public class InsertImportOnPasteTestGenerated extends AbstractInsertImportOnPasteTest {
    @TestMetadata("idea/testData/copyPaste/imports")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Copy extends AbstractInsertImportOnPasteTest {
        public void testAllFilesPresentInCopy() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/copyPaste/imports"), Pattern.compile("^([^.]+)\\.kt$"), TargetBackend.ANY, false);
        }

        @TestMetadata("AlreadyImportedExtensions.kt")
        public void testAlreadyImportedExtensions() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/AlreadyImportedExtensions.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("AlreadyImportedViaStar.kt")
        public void testAlreadyImportedViaStar() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/AlreadyImportedViaStar.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ClassAlreadyImported.kt")
        public void testClassAlreadyImported() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassAlreadyImported.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ClassMember.kt")
        public void testClassMember() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassMember.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ClassObject.kt")
        public void testClassObject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassObject.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ClassObjectFunInsideClass.kt")
        public void testClassObjectFunInsideClass() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassObjectFunInsideClass.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ClassObjectInner.kt")
        public void testClassObjectInner() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassObjectInner.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ClassResolvedToPackage.kt")
        public void testClassResolvedToPackage() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassResolvedToPackage.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ClassType.kt")
        public void testClassType() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassType.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ConflictForTypeWithTypeParameter.kt")
        public void testConflictForTypeWithTypeParameter() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ConflictForTypeWithTypeParameter.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ConflictWithClass.kt")
        public void testConflictWithClass() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ConflictWithClass.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Constructor.kt")
        public void testConstructor() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Constructor.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("DeepInnerClasses.kt")
        public void testDeepInnerClasses() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DeepInnerClasses.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("DefaultPackage.kt")
        public void testDefaultPackage() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DefaultPackage.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("DelegatedProperty.kt")
        public void testDelegatedProperty() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DelegatedProperty.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("DependenciesNotAccessibleOnPaste.kt")
        public void testDependenciesNotAccessibleOnPaste() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependenciesNotAccessibleOnPaste.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("DependencyOnJava.kt")
        public void testDependencyOnJava() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependencyOnJava.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("DependencyOnKotlinLibrary.kt")
        public void testDependencyOnKotlinLibrary() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependencyOnKotlinLibrary.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("DependencyOnStdLib.kt")
        public void testDependencyOnStdLib() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependencyOnStdLib.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("EnumEntries.kt")
        public void testEnumEntries() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/EnumEntries.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Extension.kt")
        public void testExtension() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Extension.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ExtensionAsInfixOrOperator.kt")
        public void testExtensionAsInfixOrOperator() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ExtensionAsInfixOrOperator.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ExtensionCannotBeImportedOrLengthened.kt")
        public void testExtensionCannotBeImportedOrLengthened() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ExtensionCannotBeImportedOrLengthened.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ExtensionConflict.kt")
        public void testExtensionConflict() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ExtensionConflict.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ForLoop.kt")
        public void testForLoop() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ForLoop.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/FullyQualified.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Function.kt")
        public void testFunction() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Function.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("FunctionAlreadyImported.kt")
        public void testFunctionAlreadyImported() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/FunctionAlreadyImported.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("FunctionParameter.kt")
        public void testFunctionParameter() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/FunctionParameter.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("GetExpression.kt")
        public void testGetExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/GetExpression.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ImportDependency.kt")
        public void testImportDependency() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportDependency.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ImportDirective.kt")
        public void testImportDirective() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportDirective.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ImportableEntityInExtensionLiteral.kt")
        public void testImportableEntityInExtensionLiteral() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportableEntityInExtensionLiteral.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ImportedElementCopied.kt")
        public void testImportedElementCopied() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportedElementCopied.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Inner.kt")
        public void testInner() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Inner.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Invoke.kt")
        public void testInvoke() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Invoke.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("JavaStaticViaClass.kt")
        public void testJavaStaticViaClass() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/JavaStaticViaClass.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("KT10433.kt")
        public void testKT10433() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/KT10433.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("KeywordClassName.kt")
        public void testKeywordClassName() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/KeywordClassName.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Local.kt")
        public void testLocal() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Local.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("MultiDeclaration.kt")
        public void testMultiDeclaration() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/MultiDeclaration.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("MultiReferencePartiallyCopied.kt")
        public void testMultiReferencePartiallyCopied() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/MultiReferencePartiallyCopied.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("NoImportForBuiltIns.kt")
        public void testNoImportForBuiltIns() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NoImportForBuiltIns.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("NoImportForSamePackage.kt")
        public void testNoImportForSamePackage() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NoImportForSamePackage.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("NotReferencePosition.kt")
        public void testNotReferencePosition() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NotReferencePosition.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("NotReferencePosition2.kt")
        public void testNotReferencePosition2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NotReferencePosition2.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Object.kt")
        public void testObject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Object.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("OnlyKDocReferenced.kt")
        public void testOnlyKDocReferenced() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/OnlyKDocReferenced.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("OverloadedExtensionFunction.kt")
        public void testOverloadedExtensionFunction() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/OverloadedExtensionFunction.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("PackageView.kt")
        public void testPackageView() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/PackageView.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("PartiallyQualified.kt")
        public void testPartiallyQualified() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/PartiallyQualified.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("QualifiedTypeConflict.kt")
        public void testQualifiedTypeConflict() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/QualifiedTypeConflict.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ReferencedElementAlsoCopied.kt")
        public void testReferencedElementAlsoCopied() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ReferencedElementAlsoCopied.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Super.kt")
        public void testSuper() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Super.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("ThisReference.kt")
        public void testThisReference() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ThisReference.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("TopLevelProperty.kt")
        public void testTopLevelProperty() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/TopLevelProperty.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("Trait.kt")
        public void testTrait() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Trait.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("TypeArgForUnresolvedCall.kt")
        public void testTypeArgForUnresolvedCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/TypeArgForUnresolvedCall.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("TypeParameter.kt")
        public void testTypeParameter() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/TypeParameter.kt");
            doTestCopy(fileName);
        }

        @TestMetadata("UnresolvedOverload.kt")
        public void testUnresolvedOverload() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/UnresolvedOverload.kt");
            doTestCopy(fileName);
        }
    }

    @TestMetadata("idea/testData/copyPaste/imports")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Cut extends AbstractInsertImportOnPasteTest {
        public void testAllFilesPresentInCut() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/copyPaste/imports"), Pattern.compile("^([^.]+)\\.kt$"), TargetBackend.ANY, false);
        }

        @TestMetadata("AlreadyImportedExtensions.kt")
        public void testAlreadyImportedExtensions() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/AlreadyImportedExtensions.kt");
            doTestCut(fileName);
        }

        @TestMetadata("AlreadyImportedViaStar.kt")
        public void testAlreadyImportedViaStar() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/AlreadyImportedViaStar.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ClassAlreadyImported.kt")
        public void testClassAlreadyImported() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassAlreadyImported.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ClassMember.kt")
        public void testClassMember() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassMember.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ClassObject.kt")
        public void testClassObject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassObject.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ClassObjectFunInsideClass.kt")
        public void testClassObjectFunInsideClass() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassObjectFunInsideClass.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ClassObjectInner.kt")
        public void testClassObjectInner() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassObjectInner.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ClassResolvedToPackage.kt")
        public void testClassResolvedToPackage() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassResolvedToPackage.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ClassType.kt")
        public void testClassType() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ClassType.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ConflictForTypeWithTypeParameter.kt")
        public void testConflictForTypeWithTypeParameter() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ConflictForTypeWithTypeParameter.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ConflictWithClass.kt")
        public void testConflictWithClass() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ConflictWithClass.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Constructor.kt")
        public void testConstructor() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Constructor.kt");
            doTestCut(fileName);
        }

        @TestMetadata("DeepInnerClasses.kt")
        public void testDeepInnerClasses() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DeepInnerClasses.kt");
            doTestCut(fileName);
        }

        @TestMetadata("DefaultPackage.kt")
        public void testDefaultPackage() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DefaultPackage.kt");
            doTestCut(fileName);
        }

        @TestMetadata("DelegatedProperty.kt")
        public void testDelegatedProperty() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DelegatedProperty.kt");
            doTestCut(fileName);
        }

        @TestMetadata("DependenciesNotAccessibleOnPaste.kt")
        public void testDependenciesNotAccessibleOnPaste() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependenciesNotAccessibleOnPaste.kt");
            doTestCut(fileName);
        }

        @TestMetadata("DependencyOnJava.kt")
        public void testDependencyOnJava() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependencyOnJava.kt");
            doTestCut(fileName);
        }

        @TestMetadata("DependencyOnKotlinLibrary.kt")
        public void testDependencyOnKotlinLibrary() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependencyOnKotlinLibrary.kt");
            doTestCut(fileName);
        }

        @TestMetadata("DependencyOnStdLib.kt")
        public void testDependencyOnStdLib() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/DependencyOnStdLib.kt");
            doTestCut(fileName);
        }

        @TestMetadata("EnumEntries.kt")
        public void testEnumEntries() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/EnumEntries.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Extension.kt")
        public void testExtension() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Extension.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ExtensionAsInfixOrOperator.kt")
        public void testExtensionAsInfixOrOperator() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ExtensionAsInfixOrOperator.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ExtensionCannotBeImportedOrLengthened.kt")
        public void testExtensionCannotBeImportedOrLengthened() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ExtensionCannotBeImportedOrLengthened.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ExtensionConflict.kt")
        public void testExtensionConflict() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ExtensionConflict.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ForLoop.kt")
        public void testForLoop() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ForLoop.kt");
            doTestCut(fileName);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/FullyQualified.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Function.kt")
        public void testFunction() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Function.kt");
            doTestCut(fileName);
        }

        @TestMetadata("FunctionAlreadyImported.kt")
        public void testFunctionAlreadyImported() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/FunctionAlreadyImported.kt");
            doTestCut(fileName);
        }

        @TestMetadata("FunctionParameter.kt")
        public void testFunctionParameter() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/FunctionParameter.kt");
            doTestCut(fileName);
        }

        @TestMetadata("GetExpression.kt")
        public void testGetExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/GetExpression.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ImportDependency.kt")
        public void testImportDependency() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportDependency.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ImportDirective.kt")
        public void testImportDirective() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportDirective.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ImportableEntityInExtensionLiteral.kt")
        public void testImportableEntityInExtensionLiteral() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportableEntityInExtensionLiteral.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ImportedElementCopied.kt")
        public void testImportedElementCopied() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ImportedElementCopied.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Inner.kt")
        public void testInner() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Inner.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Invoke.kt")
        public void testInvoke() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Invoke.kt");
            doTestCut(fileName);
        }

        @TestMetadata("JavaStaticViaClass.kt")
        public void testJavaStaticViaClass() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/JavaStaticViaClass.kt");
            doTestCut(fileName);
        }

        @TestMetadata("KT10433.kt")
        public void testKT10433() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/KT10433.kt");
            doTestCut(fileName);
        }

        @TestMetadata("KeywordClassName.kt")
        public void testKeywordClassName() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/KeywordClassName.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Local.kt")
        public void testLocal() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Local.kt");
            doTestCut(fileName);
        }

        @TestMetadata("MultiDeclaration.kt")
        public void testMultiDeclaration() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/MultiDeclaration.kt");
            doTestCut(fileName);
        }

        @TestMetadata("MultiReferencePartiallyCopied.kt")
        public void testMultiReferencePartiallyCopied() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/MultiReferencePartiallyCopied.kt");
            doTestCut(fileName);
        }

        @TestMetadata("NoImportForBuiltIns.kt")
        public void testNoImportForBuiltIns() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NoImportForBuiltIns.kt");
            doTestCut(fileName);
        }

        @TestMetadata("NoImportForSamePackage.kt")
        public void testNoImportForSamePackage() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NoImportForSamePackage.kt");
            doTestCut(fileName);
        }

        @TestMetadata("NotReferencePosition.kt")
        public void testNotReferencePosition() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NotReferencePosition.kt");
            doTestCut(fileName);
        }

        @TestMetadata("NotReferencePosition2.kt")
        public void testNotReferencePosition2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/NotReferencePosition2.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Object.kt")
        public void testObject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Object.kt");
            doTestCut(fileName);
        }

        @TestMetadata("OnlyKDocReferenced.kt")
        public void testOnlyKDocReferenced() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/OnlyKDocReferenced.kt");
            doTestCut(fileName);
        }

        @TestMetadata("OverloadedExtensionFunction.kt")
        public void testOverloadedExtensionFunction() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/OverloadedExtensionFunction.kt");
            doTestCut(fileName);
        }

        @TestMetadata("PackageView.kt")
        public void testPackageView() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/PackageView.kt");
            doTestCut(fileName);
        }

        @TestMetadata("PartiallyQualified.kt")
        public void testPartiallyQualified() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/PartiallyQualified.kt");
            doTestCut(fileName);
        }

        @TestMetadata("QualifiedTypeConflict.kt")
        public void testQualifiedTypeConflict() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/QualifiedTypeConflict.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ReferencedElementAlsoCopied.kt")
        public void testReferencedElementAlsoCopied() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ReferencedElementAlsoCopied.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Super.kt")
        public void testSuper() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Super.kt");
            doTestCut(fileName);
        }

        @TestMetadata("ThisReference.kt")
        public void testThisReference() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/ThisReference.kt");
            doTestCut(fileName);
        }

        @TestMetadata("TopLevelProperty.kt")
        public void testTopLevelProperty() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/TopLevelProperty.kt");
            doTestCut(fileName);
        }

        @TestMetadata("Trait.kt")
        public void testTrait() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/Trait.kt");
            doTestCut(fileName);
        }

        @TestMetadata("TypeArgForUnresolvedCall.kt")
        public void testTypeArgForUnresolvedCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/TypeArgForUnresolvedCall.kt");
            doTestCut(fileName);
        }

        @TestMetadata("TypeParameter.kt")
        public void testTypeParameter() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/TypeParameter.kt");
            doTestCut(fileName);
        }

        @TestMetadata("UnresolvedOverload.kt")
        public void testUnresolvedOverload() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/imports/UnresolvedOverload.kt");
            doTestCut(fileName);
        }
    }
}

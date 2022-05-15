/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.copy;

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
@TestMetadata("idea/testData/refactoring/copy")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class CopyTestGenerated extends AbstractCopyTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testAllFilesPresentInCopy() throws Exception {
        KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClass(this.getClass(), new File("idea/testData/refactoring/copy"), Pattern.compile("^(.+)\\.test$"), TargetBackend.ANY);
    }

    @TestMetadata("copyClassCaretInside/copyClassCaretInside.test")
    public void testCopyClassCaretInside_CopyClassCaretInside() throws Exception {
        runTest("idea/testData/refactoring/copy/copyClassCaretInside/copyClassCaretInside.test");
    }

    @TestMetadata("copyClassToExistingFile/copyClassToExistingFile.test")
    public void testCopyClassToExistingFile_CopyClassToExistingFile() throws Exception {
        runTest("idea/testData/refactoring/copy/copyClassToExistingFile/copyClassToExistingFile.test");
    }

    @TestMetadata("copyClassToNewFile/copyClassToNewFile.test")
    public void testCopyClassToNewFile_CopyClassToNewFile() throws Exception {
        runTest("idea/testData/refactoring/copy/copyClassToNewFile/copyClassToNewFile.test");
    }

    @TestMetadata("copyClassToSamePackageWithRename/copyClassToSamePackageWithRename.test")
    public void testCopyClassToSamePackageWithRename_CopyClassToSamePackageWithRename() throws Exception {
        runTest("idea/testData/refactoring/copy/copyClassToSamePackageWithRename/copyClassToSamePackageWithRename.test");
    }

    @TestMetadata("copyClassWithCompanionRefs/copyClassWithCompanionRefs.test")
    public void testCopyClassWithCompanionRefs_CopyClassWithCompanionRefs() throws Exception {
        runTest("idea/testData/refactoring/copy/copyClassWithCompanionRefs/copyClassWithCompanionRefs.test");
    }

    @TestMetadata("copyClassWithRename/copyClassWithRename.test")
    public void testCopyClassWithRename_CopyClassWithRename() throws Exception {
        runTest("idea/testData/refactoring/copy/copyClassWithRename/copyClassWithRename.test");
    }

    @TestMetadata("copyFIleFromDefaultPackage/copyFIleFromDefaultPackage.test")
    public void testCopyFIleFromDefaultPackage_CopyFIleFromDefaultPackage() throws Exception {
        runTest("idea/testData/refactoring/copy/copyFIleFromDefaultPackage/copyFIleFromDefaultPackage.test");
    }

    @TestMetadata("copyFileFromDefaultPackageToDefaultPackage/copyFIleToDefaultPackage.test")
    public void testCopyFileFromDefaultPackageToDefaultPackage_CopyFIleToDefaultPackage() throws Exception {
        runTest("idea/testData/refactoring/copy/copyFileFromDefaultPackageToDefaultPackage/copyFIleToDefaultPackage.test");
    }

    @TestMetadata("copyFIleRetainContent/copyFIleRetainContent.test")
    public void testCopyFIleRetainContent_CopyFIleRetainContent() throws Exception {
        runTest("idea/testData/refactoring/copy/copyFIleRetainContent/copyFIleRetainContent.test");
    }

    @TestMetadata("copyFIleToDefaultPackage/copyFIleToDefaultPackage.test")
    public void testCopyFIleToDefaultPackage_CopyFIleToDefaultPackage() throws Exception {
        runTest("idea/testData/refactoring/copy/copyFIleToDefaultPackage/copyFIleToDefaultPackage.test");
    }

    @TestMetadata("copyFIleWithPackageAndDirUnmatched/copyFIleWithPackageAndDirUnmatched.test")
    public void testCopyFIleWithPackageAndDirUnmatched_CopyFIleWithPackageAndDirUnmatched() throws Exception {
        runTest("idea/testData/refactoring/copy/copyFIleWithPackageAndDirUnmatched/copyFIleWithPackageAndDirUnmatched.test");
    }

    @TestMetadata("copyFunCallQualificationWithParentheses/copyFunCallQualificationWithParentheses.test")
    public void testCopyFunCallQualificationWithParentheses_CopyFunCallQualificationWithParentheses() throws Exception {
        runTest("idea/testData/refactoring/copy/copyFunCallQualificationWithParentheses/copyFunCallQualificationWithParentheses.test");
    }

    @TestMetadata("copyKtFileToTextFile/copyKtFileToTextFile.test")
    public void testCopyKtFileToTextFile_CopyKtFileToTextFile() throws Exception {
        runTest("idea/testData/refactoring/copy/copyKtFileToTextFile/copyKtFileToTextFile.test");
    }

    @TestMetadata("copyLocalClass/copyLocalClass.test")
    public void testCopyLocalClass_CopyLocalClass() throws Exception {
        runTest("idea/testData/refactoring/copy/copyLocalClass/copyLocalClass.test");
    }

    @TestMetadata("copyLocalFunction/copyLocalFunction.test")
    public void testCopyLocalFunction_CopyLocalFunction() throws Exception {
        runTest("idea/testData/refactoring/copy/copyLocalFunction/copyLocalFunction.test");
    }

    @TestMetadata("copyLocalVariable/copyLocalVariable.test")
    public void testCopyLocalVariable_CopyLocalVariable() throws Exception {
        runTest("idea/testData/refactoring/copy/copyLocalVariable/copyLocalVariable.test");
    }

    @TestMetadata("copyMemberFunction/copyMemberFunction.test")
    public void testCopyMemberFunction_CopyMemberFunction() throws Exception {
        runTest("idea/testData/refactoring/copy/copyMemberFunction/copyMemberFunction.test");
    }

    @TestMetadata("copyMemberProperty/copyMemberProperty.test")
    public void testCopyMemberProperty_CopyMemberProperty() throws Exception {
        runTest("idea/testData/refactoring/copy/copyMemberProperty/copyMemberProperty.test");
    }

    @TestMetadata("copyMultiClassFile/copyMultiClassFile.test")
    public void testCopyMultiClassFile_CopyMultiClassFile() throws Exception {
        runTest("idea/testData/refactoring/copy/copyMultiClassFile/copyMultiClassFile.test");
    }

    @TestMetadata("copyMultipleClassesToExistingFile/copyMultipleClassesToExistingFile.test")
    public void testCopyMultipleClassesToExistingFile_CopyMultipleClassesToExistingFile() throws Exception {
        runTest("idea/testData/refactoring/copy/copyMultipleClassesToExistingFile/copyMultipleClassesToExistingFile.test");
    }

    @TestMetadata("copyMultipleClassesToNewFile/copyMultipleClassesToNewFile.test")
    public void testCopyMultipleClassesToNewFile_CopyMultipleClassesToNewFile() throws Exception {
        runTest("idea/testData/refactoring/copy/copyMultipleClassesToNewFile/copyMultipleClassesToNewFile.test");
    }

    @TestMetadata("copyMultipleDeclarations/copyMultipleDeclarations.test")
    public void testCopyMultipleDeclarations_CopyMultipleDeclarations() throws Exception {
        runTest("idea/testData/refactoring/copy/copyMultipleDeclarations/copyMultipleDeclarations.test");
    }

    @TestMetadata("copyNestedClass/copyNestedClass.test")
    public void testCopyNestedClass_CopyNestedClass() throws Exception {
        runTest("idea/testData/refactoring/copy/copyNestedClass/copyNestedClass.test");
    }

    @TestMetadata("copyObject/copyObject.test")
    public void testCopyObject_CopyObject() throws Exception {
        runTest("idea/testData/refactoring/copy/copyObject/copyObject.test");
    }

    @TestMetadata("copySingleClass/copySingleClass.test")
    public void testCopySingleClass_CopySingleClass() throws Exception {
        runTest("idea/testData/refactoring/copy/copySingleClass/copySingleClass.test");
    }

    @TestMetadata("copySingleClassFile/copySingleClassFile.test")
    public void testCopySingleClassFile_CopySingleClassFile() throws Exception {
        runTest("idea/testData/refactoring/copy/copySingleClassFile/copySingleClassFile.test");
    }

    @TestMetadata("copySingleClassWithRename/copySingleClassWithRename.test")
    public void testCopySingleClassWithRename_CopySingleClassWithRename() throws Exception {
        runTest("idea/testData/refactoring/copy/copySingleClassWithRename/copySingleClassWithRename.test");
    }

    @TestMetadata("copyTopLevelFunction/copyTopLevelFunction.test")
    public void testCopyTopLevelFunction_CopyTopLevelFunction() throws Exception {
        runTest("idea/testData/refactoring/copy/copyTopLevelFunction/copyTopLevelFunction.test");
    }

    @TestMetadata("copyTopLevelFunctionWithRename/copyTopLevelFunctionWithRename.test")
    public void testCopyTopLevelFunctionWithRename_CopyTopLevelFunctionWithRename() throws Exception {
        runTest("idea/testData/refactoring/copy/copyTopLevelFunctionWithRename/copyTopLevelFunctionWithRename.test");
    }

    @TestMetadata("copyTopLevelProperty/copyTopLevelProperty.test")
    public void testCopyTopLevelProperty_CopyTopLevelProperty() throws Exception {
        runTest("idea/testData/refactoring/copy/copyTopLevelProperty/copyTopLevelProperty.test");
    }

    @TestMetadata("copyTopLevelPropertyWithRename/copyTopLevelPropertyWithRename.test")
    public void testCopyTopLevelPropertyWithRename_CopyTopLevelPropertyWithRename() throws Exception {
        runTest("idea/testData/refactoring/copy/copyTopLevelPropertyWithRename/copyTopLevelPropertyWithRename.test");
    }

    @TestMetadata("copyWithImportInsertion/copyWithImportInsertion.test")
    public void testCopyWithImportInsertion_CopyWithImportInsertion() throws Exception {
        runTest("idea/testData/refactoring/copy/copyWithImportInsertion/copyWithImportInsertion.test");
    }

    @TestMetadata("kt18149/kt18149.test")
    public void testKt18149_Kt18149() throws Exception {
        runTest("idea/testData/refactoring/copy/kt18149/kt18149.test");
    }

    @TestMetadata("protectedConstructorRefInSuperListEntry/protectedConstructorRefInSuperListEntry.test")
    public void testProtectedConstructorRefInSuperListEntry_ProtectedConstructorRefInSuperListEntry() throws Exception {
        runTest("idea/testData/refactoring/copy/protectedConstructorRefInSuperListEntry/protectedConstructorRefInSuperListEntry.test");
    }

    @TestMetadata("refToImportJavaStaticField/refToImportedJavaStaticField.test")
    public void testRefToImportJavaStaticField_RefToImportedJavaStaticField() throws Exception {
        runTest("idea/testData/refactoring/copy/refToImportJavaStaticField/refToImportedJavaStaticField.test");
    }

    @TestMetadata("refToImportJavaStaticMethod/refToImportedJavaStaticMethod.test")
    public void testRefToImportJavaStaticMethod_RefToImportedJavaStaticMethod() throws Exception {
        runTest("idea/testData/refactoring/copy/refToImportJavaStaticMethod/refToImportedJavaStaticMethod.test");
    }
}

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

package org.jetbrains.kotlin.checkers;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.dataFlow.DataFlowInspection;
import com.intellij.codeInspection.nullable.NullableStuffInspection;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.siyeh.ig.bugs.StaticCallOnSubclassInspection;
import com.siyeh.ig.bugs.StaticFieldReferenceOnSubclassInspection;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KotlinAndJavaCheckerTest extends KotlinDaemonAnalyzerTestCase {
    private static final LocalInspectionTool[] DEFAULT_TOOLS = new LocalInspectionTool[] {
            new StaticCallOnSubclassInspection(),
            new StaticFieldReferenceOnSubclassInspection(),
            new NullableStuffInspection()
    };

    private static LocalInspectionTool mapStringToTool(String toolString) {
        if ("StaticCallOnSubclassInspection".equals(toolString))
            return new StaticCallOnSubclassInspection();
        if ("StaticFieldReferenceOnSubclassInspection".equals(toolString))
            return new StaticFieldReferenceOnSubclassInspection();
        if ("NullableStuffInspection".equals(toolString))
            return new NullableStuffInspection();
        if ("DataFlowInspection".equals(toolString))
            return new DataFlowInspection();

        throw new IllegalArgumentException("Can't find inspection tool with identifier: " + toolString);
    }

    @Override
    protected LocalInspectionTool[] configureLocalInspectionTools() {
        File configureFile = new File(getTestDataPath(), getTestName(false) + ".txt");

        if (!configureFile.exists()) return DEFAULT_TOOLS;

        try {
            String configureText = FileUtil.loadFile(configureFile, true);

            InTextDirectivesUtils.assertHasUnknownPrefixes(configureText, KotlinPackage.listOf("TOOL:"));
            List<String> toolsStrings = InTextDirectivesUtils.findListWithPrefixes(configureText, "TOOL:");

            return ArrayUtil.toObjectArray(KotlinPackage.map(toolsStrings, new Function1<String, LocalInspectionTool>() {
                @Override
                public LocalInspectionTool invoke(String toolString) {
                    return mapStringToTool(toolString);
                }
            }), LocalInspectionTool.class);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.mockJdk();
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/kotlinAndJavaChecker/";
    }

    public void testName() throws Exception {
        doTest(false, false, "ClassObjects.java", "ClassObjects.kt");
    }

    public void testNoNotNullOnParameterInOverride() throws Exception {
        doTest(true, true, "NoNotNullOnParameterInOverride.java", "NoNotNullOnParameterInOverride.kt");
    }

    public void testTopLevelFunctionInDataFlowInspection() throws Exception {
        doTest(true, true, "TopLevelFunctionInDataFlowInspection.java", "TopLevelFunctionInDataFlowInspection.kt");
    }

    public void testUsingKotlinPackageDeclarations() throws Exception {
        doTest(true, true, "UsingKotlinPackageDeclarations.java", "UsingKotlinPackageDeclarations.kt");
    }

    public void testAssignKotlinClassToObjectInJava() throws Exception {
        doTest(true, true, "AssignKotlinClassToObjectInJava.java", "AssignKotlinClassToObjectInJava.kt");
    }

    public void testAssignMappedKotlinType() throws Exception {
        doTest(true, true, "AssignMappedKotlinType.java", "AssignMappedKotlinType.kt");
    }

    public void testUseKotlinSubclassesOfMappedTypes() throws Exception {
        doTest(true, true, "UseKotlinSubclassesOfMappedTypes.java", "UseKotlinSubclassesOfMappedTypes.kt");
    }

    public void testImplementedMethodsFromTraits() throws Exception {
        doTest(true, true, "ImplementedMethodsFromTraits.java", "ImplementedMethodsFromTraits.kt");
    }

    public void testEnumAutoGeneratedMethods() throws Exception {
        doTest(true, true, "EnumAutoGeneratedMethods.java", "EnumAutoGeneratedMethods.kt");
    }

    public void testEnumEntriesInSwitch() throws Exception {
        doTest(true, true, "EnumEntriesInSwitch.java", "EnumEntriesInSwitch.kt");
    }

    public void testEnumStaticImportInJava() throws Exception {
        doTest(true, true, "EnumStaticImportInJava.java", "EnumStaticImportInJava.kt");
    }
}

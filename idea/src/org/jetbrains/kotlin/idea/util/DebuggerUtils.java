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

package org.jetbrains.kotlin.idea.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.debugger.DebuggerPackage;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;

import java.util.Collection;

import static org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil.findFilesWithExactPackage;

public class DebuggerUtils {
    private DebuggerUtils() {
    }

    @Nullable
    public static JetFile findSourceFileForClass(
            @NotNull Project project,
            @NotNull GlobalSearchScope searchScope,
            @NotNull final JvmClassName className,
            @NotNull final String fileName,
            final int lineNumber
    ) {
        Collection<JetFile> filesInPackage = findFilesWithExactPackage(className.getPackageFqName(), searchScope, project);
        Collection<JetFile> filesWithExactName = Collections2.filter(filesInPackage, new Predicate<JetFile>() {
            @Override
            public boolean apply(@Nullable JetFile file) {
                return file != null && file.getName().equals(fileName);
            }
        });

        if (filesWithExactName.isEmpty()) return null;

        if (filesWithExactName.size() == 1) {
            return filesWithExactName.iterator().next();
        }

        if (!isPackagePartClassName(className)) {
            return filesWithExactName.iterator().next();
        }

        JetFile file = getFileForPackagePartPrefixedName(filesWithExactName, className.getInternalName());
        if (file != null) {
            return file;
        }

        boolean isInLibrary = KotlinPackage.all(filesWithExactName, new Function1<JetFile, Boolean>() {
            @Override
            public Boolean invoke(JetFile file) {
                return LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null;
            }
        });

        if (isInLibrary) {
            return KotlinPackage.singleOrNull(KotlinPackage.filter(filesWithExactName, new Function1<JetFile, Boolean>() {
                @Override
                public Boolean invoke(JetFile file) {
                    Integer startLineOffset = CodeInsightUtils.getStartLineOffset(file, lineNumber);
                    assert startLineOffset != null : "Cannot find start line offset for file " + file.getName() + ", line " + lineNumber;
                    JetElement elementAt = PsiTreeUtil.getParentOfType(file.findElementAt(startLineOffset), JetElement.class);
                    return elementAt != null &&
                           className.getInternalName().equals(DebuggerPackage.findPackagePartInternalNameForLibraryFile(elementAt));
                }
            }));
        }

        return null;
    }

    private static boolean isPackagePartClassName(JvmClassName className) {
        String packageName = className.getPackageFqName().asString().replaceAll("\\.", "/");
        return className.getInternalName().startsWith(packageName + "/" + PackageClassUtils.getPackageClassName(className.getPackageFqName()));
    }

    @Nullable
    private static JetFile getFileForPackagePartPrefixedName(
            @NotNull Collection<JetFile> allPackageFiles,
            @NotNull String classInternalName
    ) {
        for (JetFile file : allPackageFiles) {
            String packagePartInternalName = PackagePartClassUtils.getPackagePartInternalName(file);
            if (classInternalName.startsWith(packagePartInternalName)) {
                return file;
            }
        }
        return null;
    }
}

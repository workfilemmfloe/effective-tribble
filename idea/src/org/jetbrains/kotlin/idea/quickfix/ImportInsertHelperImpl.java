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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.NamePackage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;

import java.util.List;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class ImportInsertHelperImpl extends ImportInsertHelper {
    /**
     * Add import directive into the PSI tree for the given package.
     *
     * @param importFqn full name of the import
     * @param file File where directive should be added.
     */
    @Override
    public void addImportDirectiveIfNeeded(@NotNull FqName importFqn, @NotNull JetFile file) {
        ImportPath importPath = new ImportPath(importFqn, false);

        optimizeImportsOnTheFly(file);

        if (needImport(importPath, file)) {
            writeImportToFile(importPath, file);
        }
    }

    @Override
    public boolean optimizeImportsOnTheFly(JetFile file) {
        if (CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            new OptimizeImportsProcessor(file.getProject(), file).runWithoutProgress();
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void writeImportToFile(@NotNull ImportPath importPath, @NotNull JetFile file) {
        JetPsiFactory psiFactory = JetPsiFactory(file.getProject());
        if (file instanceof JetCodeFragment) {
            JetImportDirective newDirective = psiFactory.createImportDirective(importPath);
            ((JetCodeFragment) file).addImportsFromString(newDirective.getText());
            return;
        }

        JetImportList importList = file.getImportList();
        if (importList != null) {
            JetImportDirective newDirective = psiFactory.createImportDirective(importPath);
            importList.add(psiFactory.createNewLine());
            importList.add(newDirective);
        }
        else {
            JetImportList newDirective = psiFactory.createImportDirectiveWithImportList(importPath);
            JetPackageDirective packageDirective = file.getPackageDirective();
            if (packageDirective == null) {
                throw new IllegalStateException("Scripts are not supported: " + file.getName());
            }

            packageDirective.getParent().addAfter(newDirective, packageDirective);
        }
    }

    /**
     * Check that import is useless.
     */
    private boolean isImportedByDefault(@NotNull ImportPath importPath, @NotNull JetFile jetFile) {
        if (importPath.fqnPart().isRoot()) {
            return true;
        }

        if (!importPath.isAllUnder() && !importPath.hasAlias()) {
            // Single element import without .* and alias is useless
            if (NamePackage.isOneSegmentFQN(importPath.fqnPart())) {
                return true;
            }

            // There's no need to import a declaration from the package of current file
            if (jetFile.getPackageFqName().equals(importPath.fqnPart().parent())) {
                return true;
            }
        }

        return isImportedWithDefault(importPath, jetFile);
    }

    @Override
    public boolean isImportedWithDefault(@NotNull ImportPath importPath, @NotNull JetFile contextFile) {

        List<ImportPath> defaultImports = ProjectStructureUtil.isJsKotlinModule(contextFile)
                                   ? TopDownAnalyzerFacadeForJS.DEFAULT_IMPORTS
                                   : TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS;
        return NamePackage.isImported(importPath, defaultImports);
    }

    @Override
    public boolean needImport(@NotNull FqName fqName, @NotNull JetFile file) {
        return needImport(new ImportPath(fqName, false), file);
    }

    @Override
    public boolean needImport(@NotNull ImportPath importPath, @NotNull JetFile file) {
        return needImport(importPath, file, file.getImportDirectives());
    }

    @Override
    public boolean needImport(@NotNull ImportPath importPath, @NotNull JetFile file, List<JetImportDirective> importDirectives) {
        if (isImportedByDefault(importPath, file)) {
            return false;
        }

        if (!importDirectives.isEmpty()) {
            // Check if import is already present
            for (JetImportDirective directive : importDirectives) {
                ImportPath existentImportPath = directive.getImportPath();
                if (existentImportPath != null && NamePackage.isImported(importPath, existentImportPath)) {
                    return false;
                }
            }
        }

        return true;
    }
}

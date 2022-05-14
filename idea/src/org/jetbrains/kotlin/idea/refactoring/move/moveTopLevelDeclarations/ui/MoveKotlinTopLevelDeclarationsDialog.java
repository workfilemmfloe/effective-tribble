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

package org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.refactoring.JavaRefactoringSettings;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoChangeListener;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.move.moveClassesOrPackages.DestinationFolderComboBox;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.UniqueNameGenerator;
import com.intellij.util.ui.UIUtil;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.core.CorePackage;
import org.jetbrains.kotlin.idea.core.refactoring.RefactoringPackage;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionTable;
import org.jetbrains.kotlin.idea.refactoring.move.MovePackage;
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.*;
import org.jetbrains.kotlin.idea.util.application.ApplicationPackage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

public class MoveKotlinTopLevelDeclarationsDialog extends RefactoringDialog {
    private static final String RECENTS_KEY = "MoveKotlinTopLevelDeclarationsDialog.RECENTS_KEY";

    private static class MemberInfoModelImpl extends AbstractMemberInfoModel<JetNamedDeclaration, KotlinMemberInfo> {

    }

    private JCheckBox cbSearchInComments;
    private JCheckBox cbSearchTextOccurrences;
    private JPanel mainPanel;
    private ReferenceEditorComboWithBrowseButton classPackageChooser;
    private ComboboxWithBrowseButton destinationFolderCB;
    private JPanel targetPanel;
    private JRadioButton rbMoveToPackage;
    private JRadioButton rbMoveToFile;
    private TextFieldWithBrowseButton fileChooser;
    private JPanel memberInfoPanel;
    private JTextField tfFileNameInPackage;
    private JCheckBox cbSpecifyFileNameInPackage;
    private JCheckBox cbUpdatePackageDirective;
    private KotlinMemberSelectionTable memberTable;

    private final MoveCallback moveCallback;

    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull Set<JetNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            @Nullable JetFile targetFile,
            boolean moveToPackage,
            boolean searchInComments,
            boolean searchForTextOccurences,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        List<JetFile> sourceFiles = getSourceFiles(elementsToMove);

        this.moveCallback = moveCallback;

        init();

        setTitle(MoveHandler.REFACTORING_NAME);

        initSearchOptions(searchInComments, searchForTextOccurences);

        initPackageChooser(targetPackageName, targetDirectory, sourceFiles);

        initFileChooser(targetFile, elementsToMove, sourceFiles);

        initMoveToButtons(moveToPackage);

        initMemberInfo(elementsToMove, sourceFiles);

        updateControls();

        pack();
    }

    private static List<JetFile> getSourceFiles(@NotNull Collection<JetNamedDeclaration> elementsToMove) {
        return KotlinPackage.distinct(
                KotlinPackage.map(
                        elementsToMove,
                        new Function1<JetNamedDeclaration, JetFile>() {
                            @Override
                            public JetFile invoke(JetNamedDeclaration declaration) {
                                return declaration.getContainingJetFile();
                            }
                        }
                )
        );
    }

    @NotNull
    private static PsiDirectory getSourceDirectory(@NotNull Collection<JetFile> sourceFiles) {
        return KotlinPackage.single(
                KotlinPackage.distinct(
                        KotlinPackage.map(
                                sourceFiles,
                                new Function1<JetFile, PsiDirectory>() {
                                    @Override
                                    public PsiDirectory invoke(JetFile jetFile) {
                                        return jetFile.getParent();
                                    }
                                }
                        )
                )
        );
    }

    private static List<JetNamedDeclaration> getAllDeclarations(Collection<JetFile> sourceFiles) {
        return KotlinPackage.filterIsInstance(
                KotlinPackage.flatMap(
                        sourceFiles,
                        new Function1<JetFile, Iterable<?>>() {
                            @Override
                            public Iterable<?> invoke(JetFile jetFile) {
                                return jetFile.getDeclarations();
                            }
                        }
                ),
                JetNamedDeclaration.class
        );
    }

    private static boolean arePackagesAndDirectoryMatched(List<JetFile> sourceFiles) {
        for (JetFile sourceFile : sourceFiles) {
            if (!CorePackage.packageMatchesDirectory(sourceFile)) return false;
        }
        return true;
    }

    private void initMemberInfo(
            @NotNull final Set<JetNamedDeclaration> elementsToMove,
            @NotNull List<JetFile> sourceFiles
    ) {
        final List<KotlinMemberInfo> memberInfos = KotlinPackage.map(
                getAllDeclarations(sourceFiles),
                new Function1<JetNamedDeclaration, KotlinMemberInfo>() {
                    @Override
                    public KotlinMemberInfo invoke(JetNamedDeclaration declaration) {
                        KotlinMemberInfo memberInfo = new KotlinMemberInfo(declaration, false);
                        memberInfo.setChecked(elementsToMove.contains(declaration));
                        return memberInfo;
                    }
                }
        );
        KotlinMemberSelectionPanel selectionPanel = new KotlinMemberSelectionPanel(getTitle(), memberInfos, null);
        memberTable = selectionPanel.getTable();
        MemberInfoModelImpl memberInfoModel = new MemberInfoModelImpl();
        memberInfoModel.memberInfoChanged(new MemberInfoChange<JetNamedDeclaration, KotlinMemberInfo>(memberInfos));
        selectionPanel.getTable().setMemberInfoModel(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(
                new MemberInfoChangeListener<JetNamedDeclaration, KotlinMemberInfo>() {
                    private boolean shouldUpdateFileNameField(final Collection<KotlinMemberInfo> changedMembers) {
                        if (!tfFileNameInPackage.isEnabled()) return true;

                        Collection<JetNamedDeclaration> previousDeclarations = KotlinPackage.filterNotNull(
                                KotlinPackage.map(
                                        memberInfos,
                                        new Function1<KotlinMemberInfo, JetNamedDeclaration>() {
                                            @Override
                                            public JetNamedDeclaration invoke(KotlinMemberInfo info) {
                                                return changedMembers.contains(info) != info.isChecked() ? info.getMember() : null;
                                            }
                                        }
                                )
                        );
                        String suggestedText = previousDeclarations.isEmpty()
                                               ? ""
                                               : MovePackage.guessNewFileName(previousDeclarations);
                        return tfFileNameInPackage.getText().equals(suggestedText);
                    }

                    @Override
                    public void memberInfoChanged(MemberInfoChange<JetNamedDeclaration, KotlinMemberInfo> event) {
                        updatePackageDirectiveCheckBox();
                        updateFileNameInPackageField();
                        // Update file name field only if it user hasn't changed it to some non-default value
                        if (shouldUpdateFileNameField(event.getChangedMembers())) {
                            updateSuggestedFileName();
                        }
                    }
                }
        );
        memberInfoPanel.add(selectionPanel, BorderLayout.CENTER);
    }

    private void updateSuggestedFileName() {
        tfFileNameInPackage.setText(MovePackage.guessNewFileName(getSelectedElementsToMove()));

    }

    private void updateFileNameInPackageField() {
        boolean movingSingleFileToPackage = isMoveToPackage()
                                            && getSourceFiles(getSelectedElementsToMove()).size() == 1;
        cbSpecifyFileNameInPackage.setEnabled(movingSingleFileToPackage);
        tfFileNameInPackage.setEnabled(movingSingleFileToPackage && cbSpecifyFileNameInPackage.isSelected());
    }

    private void initPackageChooser(
            String targetPackageName,
            PsiDirectory targetDirectory,
            List<JetFile> sourceFiles
    ) {
        if (targetPackageName != null) {
            classPackageChooser.prependItem(targetPackageName);
        }

        ((DestinationFolderComboBox) destinationFolderCB).setData(
                myProject,
                targetDirectory,
                new Pass<String>() {
                    @Override
                    public void pass(String s) {
                        setErrorText(s);
                    }
                },
                classPackageChooser.getChildComponent()
        );

        cbSpecifyFileNameInPackage.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        updateFileNameInPackageField();
                    }
                }
        );

        cbUpdatePackageDirective.setSelected(arePackagesAndDirectoryMatched(sourceFiles));
    }

    private void initSearchOptions(boolean searchInComments, boolean searchForTextOccurences) {
        cbSearchInComments.setSelected(searchInComments);
        cbSearchTextOccurrences.setSelected(searchForTextOccurences);
    }

    private void initMoveToButtons(boolean moveToPackage) {
        if (moveToPackage) {
            rbMoveToPackage.setSelected(true);
        }
        else {
            rbMoveToFile.setSelected(true);
        }

        rbMoveToPackage.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        classPackageChooser.requestFocus();
                        updateControls();
                    }
                }
        );

        rbMoveToFile.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        fileChooser.requestFocus();
                        updateControls();
                    }
                }
        );
    }

    private void initFileChooser(
            @Nullable JetFile targetFile,
            @NotNull Set<JetNamedDeclaration> elementsToMove,
            @NotNull List<JetFile> sourceFiles
    ) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withRoots(ProjectRootManager.getInstance(myProject).getContentRoots())
                .withTreeRootVisible(true);

        String title = JetRefactoringBundle.message("refactoring.move.top.level.declaration.file.title");
        fileChooser.addBrowseFolderListener(title, null, myProject, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        String initialTargetPath =
                targetFile != null
                ? targetFile.getVirtualFile().getPath()
                : sourceFiles.get(0).getVirtualFile().getParent().getPath() +
                  "/" +
                  MovePackage.guessNewFileName(elementsToMove);
        fileChooser.setText(initialTargetPath);
    }

    private void createUIComponents() {
        classPackageChooser = createPackageChooser();

        destinationFolderCB = new DestinationFolderComboBox() {
            @Override
            public String getTargetPackage() {
                return MoveKotlinTopLevelDeclarationsDialog.this.getTargetPackage();
            }
        };
    }

    private ReferenceEditorComboWithBrowseButton createPackageChooser() {
        ReferenceEditorComboWithBrowseButton packageChooser =
                new PackageNameReferenceEditorCombo("", myProject, RECENTS_KEY, RefactoringBundle.message("choose.destination.package"));
        Document document = packageChooser.getChildComponent().getDocument();
        document.addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                validateButtons();
            }
        });

        return packageChooser;
    }

    private void updateControls() {
        boolean moveToPackage = isMoveToPackage();
        classPackageChooser.setEnabled(moveToPackage);
        updateFileNameInPackageField();
        fileChooser.setEnabled(!moveToPackage);
        updatePackageDirectiveCheckBox();
        UIUtil.setEnabled(targetPanel, moveToPackage && hasAnySourceRoots(), true);
        updateSuggestedFileName();
        validateButtons();
    }

    private boolean isFullFileMove() {
        Map<JetFile, List<? extends JetNamedDeclaration>> fileToElements = KotlinPackage.groupBy(
                getSelectedElementsToMove(),
                new Function1<JetNamedDeclaration, JetFile>() {
                    @Override
                    public JetFile invoke(JetNamedDeclaration declaration) {
                        return declaration.getContainingJetFile();
                    }
                }
        );
        for (Map.Entry<JetFile, List<? extends JetNamedDeclaration>> entry : fileToElements.entrySet()) {
            if (entry.getKey().getDeclarations().size() != entry.getValue().size()) return false;
        }
        return true;
    }

    private void updatePackageDirectiveCheckBox() {
        cbUpdatePackageDirective.setEnabled(isMoveToPackage() && isFullFileMove());
    }

    private boolean hasAnySourceRoots() {
        return !JavaProjectRootsUtil.getSuitableDestinationSourceRoots(myProject).isEmpty();
    }

    private void saveRefactoringSettings() {
        JavaRefactoringSettings refactoringSettings = JavaRefactoringSettings.getInstance();
        refactoringSettings.MOVE_SEARCH_IN_COMMENTS = isSearchInComments();
        refactoringSettings.MOVE_SEARCH_FOR_TEXT = isSearchInNonJavaFiles();
        refactoringSettings.MOVE_PREVIEW_USAGES = isPreviewUsages();
    }

    @Nullable
    private MoveDestination selectPackageBasedMoveDestination(boolean askIfDoesNotExist) {
        String packageName = getTargetPackage();

        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, packageName);
        PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
        if (!targetPackage.exists() && askIfDoesNotExist) {
            int ret = Messages.showYesNoDialog(myProject, RefactoringBundle.message("package.does.not.exist", packageName),
                                               RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
            if (ret != Messages.YES) return null;
        }

        return ((DestinationFolderComboBox) destinationFolderCB).selectDirectory(targetPackage, false);
    }

    private boolean checkTargetFileName(String fileName) {
        if (FileTypeManager.getInstance().getFileTypeByFileName(fileName) == JetFileType.INSTANCE) return true;
        setErrorText("Can't move to non-Kotlin file");
        return false;
    }

    @NotNull
    private static List<PsiFile> getFilesExistingInTargetDir(
            @NotNull List<JetFile> sourceFiles,
            @Nullable String targetFileName,
            @Nullable final PsiDirectory targetDirectory
    ) {
        if (targetDirectory == null) return Collections.emptyList();

        List<String> fileNames =
                targetFileName != null
                ? Collections.singletonList(targetFileName)
                : KotlinPackage.map(
                        sourceFiles,
                        new Function1<JetFile, String>() {
                            @Override
                            public String invoke(JetFile jetFile) {
                                return jetFile.getName();
                            }
                        }
                );

        return KotlinPackage.filterNotNull(
                KotlinPackage.map(
                        fileNames,
                        new Function1<String, PsiFile>() {
                            @Override
                            public PsiFile invoke(String s) {
                                return targetDirectory.findFile(s);
                            }
                        }
                )
        );
    }

    @Nullable
    private KotlinMoveTarget selectMoveTarget() {
        String message = verifyBeforeRun();
        if (message != null) {
            setErrorText(message);
            return null;
        }

        setErrorText(null);

        List<JetFile> sourceFiles = getSourceFiles(getSelectedElementsToMove());
        PsiDirectory sourceDirectory = getSourceDirectory(sourceFiles);

        if (isMoveToPackage()) {
            final MoveDestination moveDestination = selectPackageBasedMoveDestination(true);
            if (moveDestination == null) return null;

            final String targetFileName = sourceFiles.size() > 1 ? null : tfFileNameInPackage.getText();
            if (targetFileName != null && !checkTargetFileName(targetFileName)) return null;

            PsiDirectory targetDirectory = moveDestination.getTargetIfExists(sourceDirectory);

            List<PsiFile> filesExistingInTargetDir = getFilesExistingInTargetDir(sourceFiles, targetFileName, targetDirectory);
            if (!filesExistingInTargetDir.isEmpty()) {
                if (!KotlinPackage.intersect(sourceFiles, filesExistingInTargetDir).isEmpty()) {
                    setErrorText("Can't move to the original file(s)");
                    return null;
                }

                if (filesExistingInTargetDir.size() > 1) {
                    String filePathsToReport = StringUtil.join(
                            filesExistingInTargetDir,
                            new Function<PsiFile, String>() {
                                @Override
                                public String fun(PsiFile file) {
                                    return file.getVirtualFile().getPath();
                                }
                            },
                            "\n"
                    );
                    Messages.showErrorDialog(
                            myProject,
                            "Cannot perform refactoring since the following files already exist:\n\n" + filePathsToReport,
                            RefactoringBundle.message("move.title")
                    );
                    return null;
                }

                String question = String.format(
                        "File '%s' already exists. Do you want to move selected declarations to this file?",
                        filesExistingInTargetDir.get(0).getVirtualFile().getPath()
                );
                int ret =
                        Messages.showYesNoDialog(myProject, question, RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
                if (ret != Messages.YES) return null;
            }

            return new DeferredJetFileKotlinMoveTarget(
                    myProject,
                    new FqName(getTargetPackage()),
                    new Function1<JetFile, JetFile>() {
                        @Override
                        public JetFile invoke(@NotNull JetFile originalFile) {
                            return RefactoringPackage.getOrCreateKotlinFile(
                                    targetFileName != null ? targetFileName : originalFile.getName(),
                                    moveDestination.getTargetDirectory(originalFile)
                            );
                        }
                    }
            );
        }

        final File targetFile = new File(getTargetFilePath());
        if (!checkTargetFileName(targetFile.getName())) return null;
        JetFile jetFile = (JetFile) RefactoringPackage.toPsiFile(targetFile, myProject);
        if (jetFile != null) {
            if (sourceFiles.size() == 1 && sourceFiles.contains(jetFile)) {
                setErrorText("Can't move to the original file");
                return null;
            }

            return new JetFileKotlinMoveTarget(jetFile);
        }

        File targetDir = targetFile.getParentFile();
        final PsiDirectory psiDirectory = RefactoringPackage.toPsiDirectory(targetDir, myProject);
        assert psiDirectory != null : "No directory found: " + targetDir.getPath();

        PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
        if (psiPackage == null) {
            setErrorText("Could not find package corresponding to " + targetDir.getPath());
            return null;
        }

        return new DeferredJetFileKotlinMoveTarget(
                myProject,
                new FqName(psiPackage.getQualifiedName()),
                new Function1<JetFile, JetFile>() {
                    @Override
                    public JetFile invoke(@NotNull JetFile originalFile) {
                        return RefactoringPackage.getOrCreateKotlinFile(targetFile.getName(), psiDirectory);
                    }
                }
        );
    }

    @Nullable
    private String verifyBeforeRun() {
        if (memberTable.getSelectedMemberInfos().isEmpty()) return "At least one member must be selected";

        if (isMoveToPackage()) {
            String name = getTargetPackage();
            if (name.length() != 0 && !PsiNameHelper.getInstance(myProject).isQualifiedName(name)) {
                return "\'" + name + "\' is invalid destination package name";
            }
        }
        else {
            PsiFile targetFile = RefactoringPackage.toPsiFile(new File(getTargetFilePath()), myProject);
            if (!(targetFile == null || targetFile instanceof JetFile)) {
                return JetRefactoringBundle.message("refactoring.move.non.kotlin.file");
            }
        }

        if (getSourceFiles(getSelectedElementsToMove()).size() == 1 && tfFileNameInPackage.getText().isEmpty()) {
            return "File name may not be empty";
        }

        return null;
    }

    private List<JetNamedDeclaration> getSelectedElementsToMove() {
        return KotlinPackage.map(
                memberTable.getSelectedMemberInfos(),
                new Function1<KotlinMemberInfo, JetNamedDeclaration>() {
                    @Override
                    public JetNamedDeclaration invoke(KotlinMemberInfo info) {
                        return info.getMember();
                    }
                }
        );
    }

    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#" + getClass().getName();
    }

    protected final String getTargetPackage() {
        return classPackageChooser.getText().trim();
    }

    protected final String getTargetFilePath() {
        return fileChooser.getText();
    }

    @Override
    protected void canRun() throws ConfigurationException {
        String message = verifyBeforeRun();
        if (message != null) {
            throw new ConfigurationException(message);
        }
    }

    @Override
    protected void doAction() {
        KotlinMoveTarget target = selectMoveTarget();
        if (target == null) return;

        saveRefactoringSettings();

        List<JetNamedDeclaration> elementsToMove = getSelectedElementsToMove();
        final List<JetFile> sourceFiles = getSourceFiles(elementsToMove);
        final PsiDirectory sourceDirectory = getSourceDirectory(sourceFiles);

        for (PsiElement element : elementsToMove) {
            String message = target.verify(element.getContainingFile());
            if (message != null) {
                CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), message, null, myProject);
                return;
            }
        }

        try {
            boolean deleteSourceFile = false;

            if (isFullFileMove()) {
                if (isMoveToPackage()) {
                    final MoveDestination moveDestination = selectPackageBasedMoveDestination(false);
                    //noinspection ConstantConditions
                    PsiDirectory targetDir = moveDestination.getTargetIfExists(sourceDirectory);
                    final String targetFileName = sourceFiles.size() > 1 ? null : tfFileNameInPackage.getText();
                    List<PsiFile> filesExistingInTargetDir = getFilesExistingInTargetDir(sourceFiles, targetFileName, targetDir);
                    if (filesExistingInTargetDir.isEmpty()) {
                        PsiDirectory targetDirectory = ApplicationPackage.runWriteAction(
                                new Function0<PsiDirectory>() {
                                    @Override
                                    public PsiDirectory invoke() {
                                        return moveDestination.getTargetDirectory(sourceDirectory);
                                    }
                                }
                        );

                        for (JetFile sourceFile : sourceFiles) {
                            MovePackage.setUpdatePackageDirective(sourceFile, cbUpdatePackageDirective.isSelected());
                        }

                        invokeRefactoring(
                                new MoveFilesOrDirectoriesProcessor(
                                        myProject,
                                        sourceFiles.toArray(new PsiElement[sourceFiles.size()]),
                                        targetDirectory,
                                        true,
                                        isSearchInComments(),
                                        isSearchInNonJavaFiles(),
                                        new MoveCallback() {
                                            @Override
                                            public void refactoringCompleted() {
                                                try {
                                                    if (targetFileName != null) {
                                                        KotlinPackage.single(sourceFiles).setName(targetFileName);
                                                    }
                                                }
                                                finally {
                                                    if (moveCallback != null) {
                                                        moveCallback.refactoringCompleted();
                                                    }
                                                }
                                            }
                                        },
                                        EmptyRunnable.INSTANCE
                                ) {
                                    @Override
                                    protected String getCommandName() {
                                        return targetFileName != null ? "Move " + KotlinPackage.single(sourceFiles).getName() : "Move";
                                    }

                                    @Override
                                    protected void performRefactoring(@NotNull UsageInfo[] usages) {
                                        if (targetFileName != null) {
                                            JetFile sourceFile = KotlinPackage.single(sourceFiles);
                                            //noinspection ConstantConditions
                                            String temporaryName = UniqueNameGenerator.generateUniqueName(
                                                    "temp",
                                                    "",
                                                    ".kt",
                                                    KotlinPackage.map(
                                                            sourceFile.getContainingDirectory().getFiles(),
                                                            new Function1<PsiFile, String>() {
                                                                @Override
                                                                public String invoke(PsiFile file) {
                                                                    return file.getName();
                                                                }
                                                            }
                                                    )
                                            );
                                            sourceFile.setName(temporaryName);
                                        }

                                        super.performRefactoring(usages);
                                    }
                                }
                        );

                        return;
                    }
                }

                int ret = Messages.showYesNoCancelDialog(
                        myProject,
                        "You are about to move all declarations out of the source file(s). Do you want to delete empty files?",
                        RefactoringBundle.message("move.title"),
                        Messages.getQuestionIcon()
                );
                if (ret == Messages.CANCEL) return;
                deleteSourceFile = ret == Messages.YES;
            }

            MoveKotlinTopLevelDeclarationsOptions options = new MoveKotlinTopLevelDeclarationsOptions(
                    elementsToMove, target, isSearchInComments(), isSearchInNonJavaFiles(), true, deleteSourceFile, moveCallback
            );
            invokeRefactoring(new MoveKotlinTopLevelDeclarationsProcessor(myProject, options, Mover.Default.INSTANCE$));
        }
        catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), null, myProject);
        }
    }

    private boolean isSearchInNonJavaFiles() {
        return cbSearchTextOccurrences.isSelected();
    }

    private boolean isSearchInComments() {
        return cbSearchInComments.isSelected();
    }

    private boolean isMoveToPackage() {
        return rbMoveToPackage.isSelected();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return classPackageChooser.getChildComponent();
    }
}

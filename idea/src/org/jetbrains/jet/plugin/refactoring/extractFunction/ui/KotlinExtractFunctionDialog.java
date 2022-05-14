/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.extractFunction.ui;

import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.ui.EditorTextField;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle;
import org.jetbrains.jet.plugin.refactoring.RefactoringPackage;
import org.jetbrains.jet.plugin.refactoring.extractFunction.*;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;

public class KotlinExtractFunctionDialog extends DialogWrapper {
    private JPanel contentPane;
    private JPanel inputParametersPanel;
    private JComboBox visibilityBox;
    private KotlinFunctionSignatureComponent signaturePreviewField;
    private EditorTextField functionNameField;
    private JLabel functionNameLabel;
    private KotlinParameterTablePanel parameterTablePanel;

    private final Project project;

    private final ExtractionDescriptorWithConflicts originalDescriptor;
    private ExtractionDescriptor currentDescriptor;

    public KotlinExtractFunctionDialog(Project project, ExtractionDescriptorWithConflicts originalDescriptor) {
        super(project, true);

        this.project = project;
        this.originalDescriptor = originalDescriptor;
        this.currentDescriptor = originalDescriptor.getDescriptor();

        setModal(true);
        setTitle(JetRefactoringBundle.message("extract.function"));
        init();
        update();
    }

    private void createUIComponents() {
        this.signaturePreviewField = new KotlinFunctionSignatureComponent("", project);
    }

    private boolean isVisibilitySectionAvailable() {
        PsiElement target = originalDescriptor.getDescriptor().getExtractionData().getTargetSibling().getParent();
        return target instanceof JetClassBody || target instanceof JetFile;
    }

    private String getFunctionName() {
        return functionNameField.getText();
    }

    private String getVisibility() {
        if (!isVisibilitySectionAvailable()) return "";

        String value = (String) visibilityBox.getSelectedItem();
        return "internal".equals(value) ? "" : value;
    }

    private boolean checkNames() {
        if (!JetNameSuggester.isIdentifier(getFunctionName())) return false;
        for (KotlinParameterTablePanel.ParameterInfo parameterInfo : parameterTablePanel.getParameterInfos()) {
            if (!JetNameSuggester.isIdentifier(parameterInfo.getName())) return false;
        }
        return true;
    }

    private void update() {
        this.currentDescriptor = createDescriptor();

        setOKActionEnabled(checkNames());
        signaturePreviewField.setText(
                ExtractFunctionPackage.getFunctionText(currentDescriptor, false, DescriptorRenderer.SOURCE_CODE_SHORT_NAMES_IN_TYPES)
        );
    }

    @Override
    protected void init() {
        super.init();

        functionNameLabel.setLabelFor(functionNameField);

        functionNameField.setText(originalDescriptor.getDescriptor().getName());
        functionNameField.addDocumentListener(
                new DocumentAdapter() {
                    @Override
                    public void documentChanged(DocumentEvent event) {
                        update();
                    }
                }
        );

        boolean enableVisibility = isVisibilitySectionAvailable();
        visibilityBox.setEnabled(enableVisibility);
        if (enableVisibility) {
            visibilityBox.setSelectedItem("private");
        }
        visibilityBox.addItemListener(
                new ItemListener() {
                    @Override
                    public void itemStateChanged(@NotNull ItemEvent e) {
                        update();
                    }
                }
        );

        parameterTablePanel = new KotlinParameterTablePanel() {
            @Override
            protected void updateSignature() {
                KotlinExtractFunctionDialog.this.update();
            }

            @Override
            protected void onEnterAction() {
                doOKAction();
            }

            @Override
            protected void onCancelAction() {
                doCancelAction();
            }
        };
        parameterTablePanel.init(originalDescriptor.getDescriptor().getParameters());
        inputParametersPanel.add(parameterTablePanel);
    }

    @Override
    protected void doOKAction() {
        MultiMap<PsiElement, String> conflicts = ExtractFunctionPackage.validate(currentDescriptor).getConflicts();
        conflicts.values().removeAll(originalDescriptor.getConflicts().values());

        if (RefactoringPackage.checkConflictsInteractively(project, conflicts)) {
            super.doOKAction();
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return functionNameField;
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected JComponent createContentPane() {
        return contentPane;
    }

    @NotNull
    private ExtractionDescriptor createDescriptor() {
        ExtractionDescriptor descriptor = originalDescriptor.getDescriptor();

        List<KotlinParameterTablePanel.ParameterInfo> parameterInfos = parameterTablePanel.getParameterInfos();

        Map<Parameter, Parameter> oldToNewParameters = ContainerUtil.newLinkedHashMap();
        for (KotlinParameterTablePanel.ParameterInfo parameterInfo : parameterInfos) {
            oldToNewParameters.put(parameterInfo.getOriginalParameter(), parameterInfo.toParameter());
        }

        ControlFlow controlFlow = descriptor.getControlFlow();
        if (controlFlow instanceof ParameterUpdate) {
            controlFlow = new ParameterUpdate(oldToNewParameters.get(((ParameterUpdate) controlFlow).getParameter()));
        }

        Map<Integer, Replacement> replacementMap = ContainerUtil.newHashMap();
        for (Map.Entry<Integer, Replacement> e : descriptor.getReplacementMap().entrySet()) {
            Integer offset = e.getKey();
            Replacement replacement = e.getValue();

            if (replacement instanceof ParameterReplacement) {
                ParameterReplacement parameterReplacement = (ParameterReplacement) replacement;
                Parameter parameter = parameterReplacement.getParameter();

                Parameter newParameter = oldToNewParameters.get(parameter);
                if (newParameter != null) {
                    replacementMap.put(offset, parameterReplacement.copy(newParameter));
                }
            }
            else {
                replacementMap.put(offset, replacement);
            }
        }

        return new ExtractionDescriptor(
                descriptor.getExtractionData(),
                getFunctionName(),
                getVisibility(),
                ContainerUtil.newArrayList(oldToNewParameters.values()),
                descriptor.getReceiverParameter(),
                descriptor.getTypeParameters(),
                replacementMap,
                controlFlow
        );
    }

    @NotNull
    public ExtractionDescriptor getCurrentDescriptor() {
        return currentDescriptor;
    }
}

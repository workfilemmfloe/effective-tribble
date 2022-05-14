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

package org.jetbrains.kotlin.idea.run;

import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JetRunConfigurationEditor extends SettingsEditor<JetRunConfiguration> {
    private JPanel myMainPanel;
    private JTextField myMainClassField;
    private JPanel myModuleChooserHolder;
    private CommonJavaParametersPanel myCommonProgramParameters;
    private ConfigurationModuleSelector myModuleSelector;

    public JetRunConfigurationEditor(Project project) {
        LabeledComponent<JComboBox> moduleChooser = LabeledComponent.create(new JComboBox(),  "Use classpath and JDK of module:");
        myModuleChooserHolder.add(moduleChooser, BorderLayout.CENTER);
        myModuleSelector = new ConfigurationModuleSelector(project, moduleChooser.getComponent());
        myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
        moduleChooser.getComponent().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
            }
        });
    }

    @Override
    protected void resetEditorFrom(JetRunConfiguration configuration) {
        myCommonProgramParameters.reset(configuration);
        myMainClassField.setText(configuration.getRunClass() == null ? null : configuration.getRunClass());
        myModuleSelector.reset(configuration);
    }

    @Override
    protected void applyEditorTo(JetRunConfiguration configuration) throws ConfigurationException {
        myModuleSelector.applyTo(configuration);
        myCommonProgramParameters.applyTo(configuration);
        configuration.setRunClass(myMainClassField.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myMainPanel;
    }

    @Override
    protected void disposeEditor() {
    }
}

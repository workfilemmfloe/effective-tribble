/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.lang.Language;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.DifferenceFilter;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementUtil;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class KotlinCommonCodeStyleSettings extends CommonCodeStyleSettings {
    public String CODE_STYLE = null;

    public KotlinCommonCodeStyleSettings() {
        super(KotlinLanguage.INSTANCE);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        CommonCodeStyleSettings defaultSettings = getDefaultSettings();
        Set<String> supportedFields = getSupportedFields();
        if (supportedFields != null) {
            supportedFields.add("FORCE_REARRANGE_MODE");
            supportedFields.add("CODE_STYLE");
        }
        DefaultJDOMExternalizer.writeExternal(this, element, new SupportedFieldsDiffFilter(this, supportedFields, defaultSettings));
        List<Integer> softMargins = getSoftMargins();
        serializeInto(softMargins, element);

        IndentOptions myIndentOptions = getIndentOptions();
        if (myIndentOptions != null) {
            IndentOptions defaultIndentOptions = defaultSettings != null ? defaultSettings.getIndentOptions() : null;
            Element indentOptionsElement = new Element(INDENT_OPTIONS_TAG);
            myIndentOptions.serialize(indentOptionsElement, defaultIndentOptions);
            if (!indentOptionsElement.getChildren().isEmpty()) {
                element.addContent(indentOptionsElement);
            }
        }

        ArrangementSettings myArrangementSettings = getArrangementSettings();
        if (myArrangementSettings != null) {
            Element container = new Element(ARRANGEMENT_ELEMENT_NAME);
            ArrangementUtil.writeExternal(container, myArrangementSettings, myLanguage);
            if (!container.getChildren().isEmpty()) {
                element.addContent(container);
            }
        }
    }

    private void serializeInto(@NotNull List<Integer> softMargins, @NotNull Element element) {
        if (softMargins.size() > 0) {
            XmlSerializer.serializeInto(this, element);
        }
    }

    //<editor-fold desc="Copied from CommonCodeStyleSettings">
    @NonNls private static final String INDENT_OPTIONS_TAG = "indentOptions";
    @NonNls private static final String ARRANGEMENT_ELEMENT_NAME = "arrangement";

    private final Language myLanguage = KotlinLanguage.INSTANCE;

    @Nullable
    private CommonCodeStyleSettings getDefaultSettings() {
        return LanguageCodeStyleSettingsProvider.getDefaultCommonSettings(myLanguage);
    }

    @Nullable
    private Set<String> getSupportedFields() {
        final LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(myLanguage);
        return provider == null ? null : provider.getSupportedFields();
    }

    private static class SupportedFieldsDiffFilter extends DifferenceFilter<CommonCodeStyleSettings> {
        private final Set<String> mySupportedFieldNames;

        public SupportedFieldsDiffFilter(final CommonCodeStyleSettings object,
                Set<String> supportedFiledNames,
                final CommonCodeStyleSettings parentObject) {
            super(object, parentObject);
            mySupportedFieldNames = supportedFiledNames;
        }

        @Override
        public boolean isAccept(@NotNull Field field) {
            if (mySupportedFieldNames == null ||
                mySupportedFieldNames.contains(field.getName())) {
                return super.isAccept(field);
            }
            return false;
        }
    }
    //</editor-fold>
}

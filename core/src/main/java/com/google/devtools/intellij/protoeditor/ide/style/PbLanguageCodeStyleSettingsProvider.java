/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devtools.intellij.protoeditor.ide.style;

import com.google.devtools.intellij.protoeditor.ide.util.ResourceUtil;
import com.google.devtools.intellij.protoeditor.lang.PbLanguage;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public class PbLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  @NotNull
  @Override
  public Language getLanguage() {
    return PbLanguage.INSTANCE;
  }

  @Override
  public void customizeSettings(
      @NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
    if (settingsType == SettingsType.SPACING_SETTINGS) {
      consumer.showStandardOptions(
          "SPACE_WITHIN_BRACES",
          "SPACE_WITHIN_BRACKETS",
          "SPACE_WITHIN_PARENTHESIS",
          "SPACE_BEFORE_COMMA",
          "SPACE_AFTER_COMMA",
          "SPACE_AROUND_ASSIGNMENT_OPERATORS");
    } else if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
      consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE");
    } else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
      consumer.showStandardOptions("RIGHT_MARGIN", "KEEP_LINE_BREAKS");
    }
  }

  @Override
  public IndentOptionsEditor getIndentOptionsEditor() {
    return new SmartIndentOptionsEditor();
  }

  @Override
  public String getCodeSample(@NotNull SettingsType settingsType) {
    try {
      return ResourceUtil.readUrlAsString(getClass().getResource("/example.proto"));
    } catch (IOException e) {
      return "Error loading example.";
    }
  }

  @Override
  public CommonCodeStyleSettings getDefaultCommonSettings() {
    CommonCodeStyleSettings commonSettings = new CommonCodeStyleSettings(PbLanguage.INSTANCE);
    commonSettings.SPACE_BEFORE_COLON = false;
    commonSettings.SPACE_AFTER_COLON = true;

    CommonCodeStyleSettings.IndentOptions indentOptions = commonSettings.initIndentOptions();
    indentOptions.USE_TAB_CHARACTER = false;
    indentOptions.TAB_SIZE = 2;
    indentOptions.INDENT_SIZE = 2;
    indentOptions.CONTINUATION_INDENT_SIZE = 4;

    return commonSettings;
  }
}

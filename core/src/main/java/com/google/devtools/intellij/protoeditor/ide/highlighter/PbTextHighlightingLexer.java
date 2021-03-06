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
package com.google.devtools.intellij.protoeditor.ide.highlighter;

import com.google.devtools.intellij.protoeditor.lang.lexer.ProtoLexer;
import com.google.devtools.intellij.protoeditor.lang.lexer.StringLexer;
import com.google.devtools.intellij.protoeditor.lang.psi.ProtoTokenTypes;
import com.intellij.lexer.LayeredLexer;

/** Lexer used for prototext syntax highlighting. */
public class PbTextHighlightingLexer extends LayeredLexer {

  public PbTextHighlightingLexer() {
    super(ProtoLexer.forPrototext());
    registerLayer(
        StringLexer.mergingStringLexer(ProtoTokenTypes.STRING_LITERAL),
        ProtoTokenTypes.STRING_LITERAL);
  }
}

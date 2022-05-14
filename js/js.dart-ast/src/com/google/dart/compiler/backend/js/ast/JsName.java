// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.ast.metadata.HasMetadata;
import com.google.dart.compiler.common.Symbol;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract base class for named JavaScript objects.
 */
public class JsName extends HasMetadata implements Symbol {
  private static int ordinalGenerator;
  private final JsScope enclosing;
  private final int ordinal;

  @NotNull
  private final String ident;

  private final boolean temporary;

  /**
   * @param ident the unmangled ident to use for this name
   */
  JsName(JsScope enclosing, @NotNull String ident, boolean temporary) {
    this.enclosing = enclosing;
    this.ident = ident;
    this.temporary = temporary;
    ordinal = temporary ? ordinalGenerator++ : 0;
  }

  public int getOrdinal() {
    return ordinal;
  }

  public JsScope getEnclosing() {
    return enclosing;
  }

  public boolean isTemporary() {
    return temporary;
  }

  @NotNull
  public String getIdent() {
    return ident;
  }

  @NotNull
  public JsNameRef makeRef() {
    return new JsNameRef(this);
  }

  @Override
  public String toString() {
    return ident;
  }
}

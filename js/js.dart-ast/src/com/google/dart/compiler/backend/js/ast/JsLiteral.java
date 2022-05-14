// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public abstract class JsLiteral extends JsExpressionImpl {
    public static final JsValueLiteral THIS = new JsThisRef();
    public static final JsNameRef UNDEFINED = new JsNameRef("undefined");

    public static final JsNullLiteral NULL = new JsNullLiteral();

    public static final JsBooleanLiteral TRUE = new JsBooleanLiteral(true);
    public static final JsBooleanLiteral FALSE = new JsBooleanLiteral(false);

    public static JsBooleanLiteral getBoolean(boolean truth) {
        return truth ? TRUE : FALSE;
    }

    public static final class JsThisRef extends JsValueLiteral {
        private JsThisRef() {
            super();
        }

        @Override
        public void accept(JsVisitor v) {
            v.visitThis(this);
        }
    }

    public static final class JsBooleanLiteral extends JsValueLiteral {
      private final boolean value;

      // Should be interned by JsProgram
      private JsBooleanLiteral(boolean value) {
        this.value = value;
      }

      public boolean getValue() {
        return value;
      }

      @Override
      public void accept(JsVisitor v) {
        v.visitBoolean(this);
      }
    }

    /**
     * A JavaScript string literal expression.
     */
    public abstract static class JsValueLiteral extends JsLiteral {
        protected JsValueLiteral() {
        }

        @Override
        public final boolean isLeaf() {
            return true;
        }
    }
}

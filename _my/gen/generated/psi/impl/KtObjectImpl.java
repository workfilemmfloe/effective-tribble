// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static generated.KotlinTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import generated.psi.*;

public class KtObjectImpl extends ASTWrapperPsiElement implements KtObject {

  public KtObjectImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitObject(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtClassBody getClassBody() {
    return findChildByClass(KtClassBody.class);
  }

  @Override
  @Nullable
  public KtDelegationSpecifierExt getDelegationSpecifierExt() {
    return findChildByClass(KtDelegationSpecifierExt.class);
  }

  @Override
  @Nullable
  public KtModifierList getModifierList() {
    return findChildByClass(KtModifierList.class);
  }

  @Override
  @NotNull
  public KtObjectName getObjectName() {
    return findNotNullChildByClass(KtObjectName.class);
  }

}

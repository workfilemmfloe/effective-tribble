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

public class KtStatementsBlockImpl extends ASTWrapperPsiElement implements KtStatementsBlock {

  public KtStatementsBlockImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitStatementsBlock(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtCharacterConstant> getCharacterConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtCharacterConstant.class);
  }

  @Override
  @NotNull
  public List<KtFloatConstant> getFloatConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFloatConstant.class);
  }

  @Override
  @NotNull
  public List<KtIntegerConstant> getIntegerConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtIntegerConstant.class);
  }

  @Override
  @NotNull
  public List<KtNull> getNullList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtNull.class);
  }

  @Override
  @NotNull
  public List<KtAdditiveExpressionPlus> getAdditiveExpressionPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAdditiveExpressionPlus.class);
  }

  @Override
  @NotNull
  public List<KtAnnotatedExpression> getAnnotatedExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotatedExpression.class);
  }

  @Override
  @NotNull
  public List<KtArrayAccess> getArrayAccessList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtArrayAccess.class);
  }

  @Override
  @NotNull
  public List<KtAssignmentExpression> getAssignmentExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAssignmentExpression.class);
  }

  @Override
  @NotNull
  public List<KtAtomicPackage> getAtomicPackageList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAtomicPackage.class);
  }

  @Override
  @NotNull
  public List<KtBinaryConstant> getBinaryConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtBinaryConstant.class);
  }

  @Override
  @NotNull
  public List<KtCallSuffix> getCallSuffixList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtCallSuffix.class);
  }

  @Override
  @NotNull
  public List<KtCallableReference> getCallableReferenceList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtCallableReference.class);
  }

  @Override
  @NotNull
  public List<KtClassDeclarationLocal> getClassDeclarationLocalList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtClassDeclarationLocal.class);
  }

  @Override
  @NotNull
  public List<KtComparisonExpression> getComparisonExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtComparisonExpression.class);
  }

  @Override
  @NotNull
  public List<KtConjunctionPlus> getConjunctionPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtConjunctionPlus.class);
  }

  @Override
  @NotNull
  public List<KtDisjunctionPlus> getDisjunctionPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtDisjunctionPlus.class);
  }

  @Override
  @NotNull
  public List<KtDoWhileLoop> getDoWhileLoopList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtDoWhileLoop.class);
  }

  @Override
  @NotNull
  public List<KtDotQualifiedExpression> getDotQualifiedExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtDotQualifiedExpression.class);
  }

  @Override
  @NotNull
  public List<KtElvisAccessExpression> getElvisAccessExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtElvisAccessExpression.class);
  }

  @Override
  @NotNull
  public List<KtEqualityComparisonExpression> getEqualityComparisonExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtEqualityComparisonExpression.class);
  }

  @Override
  @NotNull
  public List<KtFieldIdentifierReference> getFieldIdentifierReferenceList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFieldIdentifierReference.class);
  }

  @Override
  @NotNull
  public List<KtForLoop> getForLoopList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtForLoop.class);
  }

  @Override
  @NotNull
  public List<KtFunction> getFunctionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFunction.class);
  }

  @Override
  @NotNull
  public List<KtFunctionLiteralExpression> getFunctionLiteralExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFunctionLiteralExpression.class);
  }

  @Override
  @NotNull
  public List<KtIfExpression> getIfExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtIfExpression.class);
  }

  @Override
  @NotNull
  public List<KtInfixFunctionCallPlus> getInfixFunctionCallPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtInfixFunctionCallPlus.class);
  }

  @Override
  @NotNull
  public List<KtJumpBreak> getJumpBreakList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpBreak.class);
  }

  @Override
  @NotNull
  public List<KtJumpContinue> getJumpContinueList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpContinue.class);
  }

  @Override
  @NotNull
  public List<KtJumpReturn> getJumpReturnList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpReturn.class);
  }

  @Override
  @NotNull
  public List<KtJumpThrow> getJumpThrowList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpThrow.class);
  }

  @Override
  @NotNull
  public List<KtLabel> getLabelList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLabel.class);
  }

  @Override
  @NotNull
  public List<KtLabelExpression> getLabelExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLabelExpression.class);
  }

  @Override
  @NotNull
  public List<KtMultiplicativeExpressionPlus> getMultiplicativeExpressionPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtMultiplicativeExpressionPlus.class);
  }

  @Override
  @NotNull
  public List<KtNamedInfixFirst> getNamedInfixFirstList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtNamedInfixFirst.class);
  }

  @Override
  @NotNull
  public List<KtNamedInfixPlus> getNamedInfixPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtNamedInfixPlus.class);
  }

  @Override
  @NotNull
  public List<KtObject> getObjectList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtObject.class);
  }

  @Override
  @NotNull
  public List<KtObjectLiteral> getObjectLiteralList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtObjectLiteral.class);
  }

  @Override
  @NotNull
  public List<KtParenthesizedExpression> getParenthesizedExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtParenthesizedExpression.class);
  }

  @Override
  @NotNull
  public List<KtPlusPlusAndOthersExpression> getPlusPlusAndOthersExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtPlusPlusAndOthersExpression.class);
  }

  @Override
  @NotNull
  public List<KtPrefixOperationExpression> getPrefixOperationExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtPrefixOperationExpression.class);
  }

  @Override
  @NotNull
  public List<KtPropertyLocal> getPropertyLocalList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtPropertyLocal.class);
  }

  @Override
  @NotNull
  public List<KtRangeExpressionPlus> getRangeExpressionPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtRangeExpressionPlus.class);
  }

  @Override
  @NotNull
  public List<KtReferenceExpression> getReferenceExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtReferenceExpression.class);
  }

  @Override
  @NotNull
  public List<KtSafeAccessExpression> getSafeAccessExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtSafeAccessExpression.class);
  }

  @Override
  @NotNull
  public List<KtStringTemplate> getStringTemplateList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtStringTemplate.class);
  }

  @Override
  @NotNull
  public List<KtSuperExpression> getSuperExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtSuperExpression.class);
  }

  @Override
  @NotNull
  public List<KtThisExpression> getThisExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtThisExpression.class);
  }

  @Override
  @NotNull
  public List<KtTryBlock> getTryBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtTryBlock.class);
  }

  @Override
  @NotNull
  public List<KtTypeRHSPlus> getTypeRHSPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtTypeRHSPlus.class);
  }

  @Override
  @NotNull
  public List<KtTypedef> getTypedefList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtTypedef.class);
  }

  @Override
  @NotNull
  public List<KtWhen> getWhenList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtWhen.class);
  }

  @Override
  @NotNull
  public List<KtWhileLoop> getWhileLoopList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtWhileLoop.class);
  }

}

package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageBaseVisitor;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import java.util.HashSet;
import java.util.Set;

public class FieldExtractorVisitor extends RuleFlowLanguageBaseVisitor<Void> {

  private final Set<String> inputFields = new HashSet<>();
  private final Set<String> featureFields = new HashSet<>();
  private final Set<String> listNames = new HashSet<>();
  private final Set<String> functionNames = new HashSet<>();
  private final Set<String> variableNames = new HashSet<>();

  public Set<String> getInputFields() {
    return inputFields;
  }

  public Set<String> getFeatureFields() {
    return featureFields;
  }

  public Set<String> getListNames() {
    return listNames;
  }

  public Set<String> getFunctionNames() {
    return functionNames;
  }

  public Set<String> getVariableNames() {
    return variableNames;
  }

  @Override
  public Void visitCustomFunctionCall(RuleFlowLanguageParser.CustomFunctionCallContext ctx) {
    functionNames.add(ctx.ID().getText());
    return visitChildren(ctx);
  }

  @Override
  public Void visitSet_clause(RuleFlowLanguageParser.Set_clauseContext ctx) {
    variableNames.add(ctx.variable.getText().substring(1)); // strip "$"
    return visitChildren(ctx);
  }

  @Override
  public Void visitVariableRef(RuleFlowLanguageParser.VariableRefContext ctx) {
    return null; // variable references are not input fields
  }

  @Override
  public Void visitMemberAccess(RuleFlowLanguageParser.MemberAccessContext ctx) {
    String fullPath = buildPropertyPath(ctx);
    if (fullPath != null) {
      if (fullPath.startsWith("features.")) {
        featureFields.add(fullPath.substring("features.".length()));
      } else {
        inputFields.add(fullPath);
      }
      return null; // full path handled, don't descend
    }
    // Non-property base (e.g. function call) — visit children to capture arg fields
    return visitChildren(ctx);
  }

  private String buildPropertyPath(RuleFlowLanguageParser.MemberAccessContext ctx) {
    String basePath;
    if (ctx.base instanceof RuleFlowLanguageParser.PropertyContext) {
      basePath = ((RuleFlowLanguageParser.PropertyContext) ctx.base).validProperty().getText();
    } else if (ctx.base instanceof RuleFlowLanguageParser.MemberAccessContext) {
      basePath = buildPropertyPath((RuleFlowLanguageParser.MemberAccessContext) ctx.base);
      if (basePath == null) return null;
    } else {
      return null; // Non-property base (function call, arithmetic, etc.)
    }
    if (basePath.startsWith(".")) basePath = basePath.substring(1);
    return basePath + "." + ctx.field.getText();
  }

  @Override
  public Void visitValidProperty(RuleFlowLanguageParser.ValidPropertyContext ctx) {
    String propertyText = ctx.getText();

    if (propertyText.startsWith(".")) {
      propertyText = propertyText.substring(1);
    }

    if (propertyText.startsWith("features.")) {
      featureFields.add(propertyText.substring("features.".length()));
    } else {
      inputFields.add(propertyText);
    }

    return null;
  }

  @Override
  public Void visitListElems(RuleFlowLanguageParser.ListElemsContext ctx) {
    if (ctx.storedList != null && ctx.string_literal() != null) {
      String name = ctx.string_literal().get(0).getText();
      listNames.add(stripQuotes(name));
    }
    return null;
  }

  @Override
  public Void visitEvalInList(RuleFlowLanguageParser.EvalInListContext ctx) {
    if (ctx.listName != null) {
      listNames.add(stripQuotes(ctx.listName.getText()));
    }
    // Continue visiting children to extract fields from the predicate expression
    return visitChildren(ctx);
  }

  private String stripQuotes(String quotedString) {
    if (quotedString.startsWith("'") && quotedString.endsWith("'")) {
      return quotedString.substring(1, quotedString.length() - 1);
    }
    return quotedString;
  }
}
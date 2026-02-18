package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageBaseVisitor;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import java.util.HashSet;
import java.util.Set;

public class FieldExtractorVisitor extends RuleFlowLanguageBaseVisitor<Void> {

  private final Set<String> inputFields = new HashSet<>();
  private final Set<String> featureFields = new HashSet<>();
  private final Set<String> listNames = new HashSet<>();

  public Set<String> getInputFields() {
    return inputFields;
  }

  public Set<String> getFeatureFields() {
    return featureFields;
  }

  public Set<String> getListNames() {
    return listNames;
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
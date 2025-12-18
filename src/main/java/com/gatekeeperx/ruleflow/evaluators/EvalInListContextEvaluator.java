package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class EvalInListContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.EvalInListContext> {
    private static final Logger logger = LoggerFactory.getLogger(EvalInListContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.EvalInListContext ctx, Visitor visitor)
        throws PropertyNotFoundException, UnexpectedSymbolException {
        
        // Extract list name from string literal
        String listName = stripQuotes(ctx.listName.getText());
        
        // Get the list from visitor
        List<?> list = visitor.getLists().get(listName);
        if (list == null) {
            logger.warn("List '{}' not found", listName);
            return false;
        }

        // Evaluate predicate for each item in the list
        // Return true if any item matches the predicate
        boolean result = list.stream().anyMatch(item -> {
            try {
                // Create a scoped visitor where the current item is the data context
                // This allows elem.field1 to access field1 from the current item
                ScopedVisitor scopedVisitor = new ScopedVisitor(item, visitor);
                Object predicateResult = scopedVisitor.visit(ctx.predicate);
                boolean match = predicateResult instanceof Boolean && (Boolean) predicateResult;
                logger.debug("EvalInList predicate evaluation: item={}, result={}, match={}", item, predicateResult, match);
                return match;
            } catch (Exception e) {
                logger.warn("Error evaluating predicate for list item {}: {}", item, e.getMessage(), e);
                return false;
            }
        });

        logger.debug("EvalInList: listName={}, result={}", listName, result);
        return result;
    }

    private String stripQuotes(String quotedString) {
        if (quotedString.startsWith("'") && quotedString.endsWith("'")) {
            return quotedString.substring(1, quotedString.length() - 1);
        }
        return quotedString;
    }

    /**
     * Scoped visitor that handles property access with 'elem.' prefix.
     * When a property like 'elem.field1' is accessed, it resolves to 'field1' from the current list item.
     */
    private static class ScopedVisitor extends Visitor {
        private final Object currentItem;
        private final Visitor parentVisitor;

        public ScopedVisitor(Object currentItem, Visitor parentVisitor) {
            // Use the current item as data context, preserve root and lists from parent
            super(
                currentItem instanceof Map ? (Map<String, ?>) currentItem : Map.of(),
                parentVisitor.getLists(),
                parentVisitor.getRoot()
            );
            this.currentItem = currentItem;
            this.parentVisitor = parentVisitor;
        }

        @Override
        public Object visit(org.antlr.v4.runtime.tree.ParseTree tree) {
            // Intercept PropertyContext and ValidPropertyContext to handle 'elem' before normal dispatch
            if (tree instanceof RuleFlowLanguageParser.PropertyContext) {
                // PropertyContext wraps ValidPropertyContext, so extract and handle it
                RuleFlowLanguageParser.PropertyContext propCtx = (RuleFlowLanguageParser.PropertyContext) tree;
                return visitValidProperty(propCtx.validProperty());
            }
            if (tree instanceof RuleFlowLanguageParser.ValidPropertyContext) {
                return visitValidProperty((RuleFlowLanguageParser.ValidPropertyContext) tree);
            }
            // For all other contexts, use super.visit() which will use this visitor's context
            // but can access parent's lists and root through getLists() and getRoot()
            return super.visit(tree);
        }

        @Override
        public Object visitValidProperty(RuleFlowLanguageParser.ValidPropertyContext ctx) {
            logger.debug("ScopedVisitor.visitValidProperty: property={}, K_ELEM.size={}, ID.size={}, nestedProperty={}, property={}, currentItem={}", 
                ctx.getText(), ctx.K_ELEM().size(), ctx.ID().size(), ctx.nestedProperty != null, ctx.property != null, currentItem);
            
            // Check if this property starts with 'elem' token
            boolean startsWithElem = ctx.K_ELEM().size() > 0 && "elem".equals(ctx.K_ELEM(0).getText());
            logger.debug("ScopedVisitor.visitValidProperty: startsWithElem={}", startsWithElem);
            
            if (startsWithElem) {
                int tokenCount = getTokenCount(ctx);
                logger.debug("ScopedVisitor.visitValidProperty: tokenCount={}", tokenCount);
                
                // If just 'elem' (single token), return the current item itself
                if (tokenCount == 1) {
                    logger.debug("ScopedVisitor: 'elem' refers to current item: {}", currentItem);
                    return currentItem;
                }
                
                // Handle 'elem.field1' or 'elem.field1.field2' patterns
                // Skip the first 'elem' token and resolve the rest from the current item
                if (currentItem instanceof Map) {
                    Map<String, ?> itemMap = (Map<String, ?>) currentItem;
                    Object result = resolveNestedProperty(ctx, itemMap, 1); // Start from index 1 (skip 'elem')
                    logger.debug("ScopedVisitor resolved nested property elem.*: {}", result);
                    return result;
                } else {
                    throw new PropertyNotFoundException("Property 'elem." + getPropertyPath(ctx, 1) + "' cannot be found - item is not a map");
                }
            }
            
            // For simple properties (not starting with 'elem'), first try current item, then fall back to parent
            if (ctx.property != null && currentItem instanceof Map) {
                Map<String, ?> itemMap = (Map<String, ?>) currentItem;
                String propertyName = getFirstTokenText(ctx);
                if (itemMap.containsKey(propertyName)) {
                    Object result = itemMap.get(propertyName);
                    logger.debug("ScopedVisitor resolved simple property from item: {}={}", propertyName, result);
                    return result;
                }
            }
            
            // Fall back to parent visitor's property resolution
            logger.debug("ScopedVisitor falling back to parent visitor for property: {}", ctx.getText());
            return parentVisitor.visit(ctx);
        }

        private String getFirstTokenText(RuleFlowLanguageParser.ValidPropertyContext ctx) {
            // Check K_ELEM first (since it's more specific), then ID
            if (ctx.K_ELEM().size() > 0) {
                return ctx.K_ELEM(0).getText();
            } else if (ctx.ID().size() > 0) {
                return ctx.ID(0).getText();
            }
            return "";
        }

        private int getTokenCount(RuleFlowLanguageParser.ValidPropertyContext ctx) {
            return ctx.ID().size() + ctx.K_ELEM().size();
        }

        @SuppressWarnings("unchecked")
        private Object resolveNestedProperty(RuleFlowLanguageParser.ValidPropertyContext ctx, Map<String, ?> data, int startIndex) {
            Map<String, ?> currentData = data;
            var allTokens = getAllTokens(ctx);
            logger.debug("ScopedVisitor.resolveNestedProperty: allTokens={}, startIndex={}, data={}", allTokens, startIndex, data);
            
            for (int i = startIndex; i < allTokens.size(); i++) {
                String part = allTokens.get(i);
                Object value = currentData.get(part);
                logger.debug("ScopedVisitor.resolveNestedProperty: part={}, value={}, currentData={}", part, value, currentData);
                
                if (value == null) {
                    throw new PropertyNotFoundException("Property 'elem." + getPropertyPath(ctx, startIndex) + "' cannot be found at '" + part + "'");
                }
                
                if (i == allTokens.size() - 1) {
                    // Last part, return the value
                    return value;
                } else if (value instanceof Map<?, ?>) {
                    // Continue navigating nested maps
                    currentData = (Map<String, ?>) value;
                } else {
                    // Not a map, cannot navigate further
                    throw new PropertyNotFoundException("Property 'elem." + getPropertyPath(ctx, startIndex) + "' cannot be navigated - '" + part + "' is not a map");
                }
            }
            
            throw new PropertyNotFoundException("Property 'elem." + getPropertyPath(ctx, startIndex) + "' cannot be found");
        }

        private List<String> getAllTokens(RuleFlowLanguageParser.ValidPropertyContext ctx) {
            List<String> tokens = new java.util.ArrayList<>();
            // For nested properties, tokens appear in order: first token, DOT, second token, DOT, etc.
            // We need to preserve the order as they appear in the parse tree
            // K_ELEM tokens appear before ID tokens in the property path
            int totalTokens = ctx.K_ELEM().size() + ctx.ID().size();
            
            // For elem.field1, we have: K_ELEM("elem") at index 0, ID("field1") at index 1
            // So we add K_ELEM tokens first, then ID tokens
            for (int i = 0; i < ctx.K_ELEM().size(); i++) {
                tokens.add(ctx.K_ELEM(i).getText());
            }
            for (int i = 0; i < ctx.ID().size(); i++) {
                tokens.add(ctx.ID(i).getText());
            }
            
            return tokens;
        }

        private String getPropertyPath(RuleFlowLanguageParser.ValidPropertyContext ctx, int startIndex) {
            var allTokens = getAllTokens(ctx);
            StringBuilder path = new StringBuilder();
            for (int i = startIndex; i < allTokens.size(); i++) {
                if (i > startIndex) {
                    path.append(".");
                }
                path.append(allTokens.get(i));
            }
            return path.toString();
        }

    }
}



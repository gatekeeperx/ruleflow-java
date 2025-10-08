package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class SyntaxValidatorVisitor {
  public static List<String> validate(String input) {
    CharStream charStream = CharStreams.fromString(input);
    RuleFlowLanguageLexer lexer = new RuleFlowLanguageLexer(charStream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    RuleFlowLanguageParser parser = new RuleFlowLanguageParser(tokens);

    List<String> errors = new ArrayList<>();
    parser.removeErrorListeners(); // remove default console error listener
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(
          Recognizer<?, ?> recognizer,
          Object offendingSymbol,
          int line,
          int charPositionInLine,
          String msg,
          RecognitionException e
      ) {
        errors.add("line " + line + ":" + charPositionInLine + " " + msg);
      }
    });

    parser.parse(); // start parsing
    return errors;
  }

}

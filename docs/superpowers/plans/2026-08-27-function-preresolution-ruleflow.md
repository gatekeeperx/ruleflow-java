# Function Pre-Resolution — ruleflow-java Implementation Plan (Plan A)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add additive library capabilities to ruleflow-java so slow custom functions can be pre-resolved before evaluation: a canonical key, an extract of resolved call sites, and an evaluate() overload that injects pre-resolved results.

**Architecture:** A shared canonical string key identifies a `(functionName, resolvedArgs)` pair on both sides of the async boundary. `Workflow.extractFunctionCalls(payload, lists)` returns every custom-function call site with its arguments already evaluated against the payload, each carrying its key. A new `Workflow.evaluate(payload, lists, functions, resolvedFunctions)` overload seeds a key→result map into the evaluator; `CustomFunctionCallContextEvaluator` checks it first and skips the function call on a hit. All existing APIs delegate with an empty map, so nothing changes for current callers.

**Tech Stack:** Java, ANTLR4-generated parser/visitors, JUnit 5, Maven.

## Global Constraints
- Purely additive: existing `Workflow.evaluate(...)` overloads and their behavior MUST NOT change.
- The DSL grammar is NOT modified; `screening(...)` keeps its exact syntax.
- Canonical key must be deterministic and produce identical output at extract time and eval time for the same resolved args.
- Version bump: `0.18.0 -> 0.19.0` (minor, additive).
- Follow existing test style in `src/test/java` (JUnit 5, no package, `Workflow`-driven end-to-end tests plus focused unit tests).

---

### Task A1: Canonical function-call key

**Files:**
- Create: `src/main/java/com/gatekeeperx/ruleflow/functions/RuleflowFunctionKey.java`
- Test: `src/test/java/RuleflowFunctionKeyTest.java`

**Interfaces:**
- Produces: `public static String RuleflowFunctionKey.of(String functionName, Map<String,Object> resolvedArgs)` — deterministic, serializable string key. Used by Task A2 (extract) and Task A3 (evaluator lookup).

- [ ] **Step 1: Write the failing test**

```java
import com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleflowFunctionKeyTest {

    @Test
    public void sameFunctionAndArgsProduceSameKey() {
        Map<String,Object> a = Map.of("0", "DOC123", "1", "John Doe");
        Map<String,Object> b = Map.of("0", "DOC123", "1", "John Doe");
        Assertions.assertEquals(
            RuleflowFunctionKey.of("screening", a),
            RuleflowFunctionKey.of("screening", b));
    }

    @Test
    public void keyIsIndependentOfArgInsertionOrder() {
        Map<String,Object> ordered = new LinkedHashMap<>();
        ordered.put("documentNumber", "DOC123");
        ordered.put("fullName", "John Doe");
        Map<String,Object> reversed = new LinkedHashMap<>();
        reversed.put("fullName", "John Doe");
        reversed.put("documentNumber", "DOC123");
        Assertions.assertEquals(
            RuleflowFunctionKey.of("screening", ordered),
            RuleflowFunctionKey.of("screening", reversed));
    }

    @Test
    public void differentArgsProduceDifferentKey() {
        Assertions.assertNotEquals(
            RuleflowFunctionKey.of("screening", Map.of("0", "DOC123")),
            RuleflowFunctionKey.of("screening", Map.of("0", "DOC999")));
    }

    @Test
    public void differentFunctionNameProducesDifferentKey() {
        Map<String,Object> args = Map.of("0", "x");
        Assertions.assertNotEquals(
            RuleflowFunctionKey.of("screening", args),
            RuleflowFunctionKey.of("score", args));
    }

    @Test
    public void handlesNestedMapsAndListsDeterministically() {
        Map<String,Object> nested = Map.of("0", Map.of("b", 2, "a", List.of(1, 2, 3)));
        Assertions.assertEquals(
            RuleflowFunctionKey.of("f", nested),
            RuleflowFunctionKey.of("f", Map.of("0", Map.of("a", List.of(1, 2, 3), "b", 2))));
    }

    @Test
    public void handlesNullArgValues() {
        Map<String,Object> args = new LinkedHashMap<>();
        args.put("0", null);
        // must not throw
        Assertions.assertNotNull(RuleflowFunctionKey.of("f", args));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=RuleflowFunctionKeyTest test`
Expected: FAIL — `RuleflowFunctionKey` does not exist / cannot compile.

- [ ] **Step 3: Write minimal implementation**

```java
package com.gatekeeperx.ruleflow.functions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Produces a deterministic, serializable identity for a resolved custom-function
 * call. The key is stable across processes and time for the same
 * (functionName, resolvedArgs), so a pre-resolved result computed asynchronously
 * can be looked up at evaluation time.
 */
public final class RuleflowFunctionKey {

    private RuleflowFunctionKey() {}

    public static String of(String functionName, Map<String, Object> resolvedArgs) {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append('(');
        canonicalize(resolvedArgs, sb);
        sb.append(')');
        return functionName + ":" + sha256(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private static void canonicalize(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            ((Map<Object, Object>) value).forEach((k, v) -> sorted.put(String.valueOf(k), v));
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : sorted.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(e.getKey()).append('=');
                canonicalize(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object item : (List<Object>) value) {
                if (!first) sb.append(',');
                first = false;
                canonicalize(item, sb);
            }
            sb.append(']');
        } else {
            // Include the type so 5 (Integer) and "5" (String) never collide.
            sb.append(value.getClass().getSimpleName()).append(':').append(value);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=RuleflowFunctionKeyTest test`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gatekeeperx/ruleflow/functions/RuleflowFunctionKey.java src/test/java/RuleflowFunctionKeyTest.java
git commit -m "feat: add deterministic canonical key for custom-function calls"
```

---

### Task A2: Extract resolved function-call sites

**Files:**
- Create: `src/main/java/com/gatekeeperx/ruleflow/vo/PendingFunctionCall.java`
- Create: `src/main/java/com/gatekeeperx/ruleflow/visitors/FunctionCallExtractorVisitor.java`
- Modify: `src/main/java/com/gatekeeperx/ruleflow/evaluators/CustomFunctionCallContextEvaluator.java` (extract a reusable static `buildArgs`)
- Modify: `src/main/java/com/gatekeeperx/ruleflow/Workflow.java` (add `extractFunctionCalls`)
- Test: `src/test/java/FunctionCallExtractorTest.java`

**Interfaces:**
- Consumes: `RuleflowFunctionKey.of(...)` from Task A1.
- Produces:
  - `PendingFunctionCall { String getFunctionName(); Map<String,Object> getArgs(); String getKey(); }` with an all-args constructor.
  - `static Map<String,Object> CustomFunctionCallContextEvaluator.buildArgs(RuleFlowLanguageParser.CustomFunctionCallContext ctx, Visitor visitor)` — evaluates each argument expression to a value.
  - `List<PendingFunctionCall> Workflow.extractFunctionCalls(Map<String,Object> payload, Map<String,List<?>> lists)` — one entry per distinct key; call sites whose args cannot be resolved without calling another function (nested async) are skipped with a logged warning.

- [ ] **Step 1: Write the failing test**

```java
import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.PendingFunctionCall;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class FunctionCallExtractorTest {

    private static final String WF = """
        workflow 'test'
            ruleset 'sanctions'
                'hit' screening(customer.document, customer.name + ' ' + customer.lastName).matchCount > 0 return block
            default allow
        end
        """;

    @Test
    public void extractsResolvedArgsForScreening() {
        List<PendingFunctionCall> calls = new Workflow(WF).extractFunctionCalls(
            Map.of("customer", Map.of("document", "DOC1", "name", "John", "lastName", "Doe")),
            Map.of());

        Assertions.assertEquals(1, calls.size());
        PendingFunctionCall call = calls.get(0);
        Assertions.assertEquals("screening", call.getFunctionName());
        // positional args, concatenation already resolved
        Assertions.assertEquals("DOC1", call.getArgs().get("0"));
        Assertions.assertEquals("John Doe", call.getArgs().get("1"));
        Assertions.assertNotNull(call.getKey());
    }

    @Test
    public void deduplicatesIdenticalCalls() {
        String wf = """
            workflow 'test'
                ruleset 'a'
                    'r1' screening(customer.document).matchCount > 0 return block
                    'r2' screening(customer.document).matchCount > 5 return review
                default allow
            end
            """;
        List<PendingFunctionCall> calls = new Workflow(wf).extractFunctionCalls(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of());
        Assertions.assertEquals(1, calls.size());
    }

    @Test
    public void returnsEmptyWhenNoCustomFunctions() {
        String wf = """
            workflow 'test'
                ruleset 'a'
                    'r1' amount > 100 return block
                default allow
            end
            """;
        List<PendingFunctionCall> calls = new Workflow(wf).extractFunctionCalls(
            Map.of("amount", 50), Map.of());
        Assertions.assertTrue(calls.isEmpty());
    }

    @Test
    public void keyMatchesEvaluationTimeKey() {
        // The key produced by the extract must equal the key the evaluator will
        // compute from the same payload, so a hit occurs at evaluate time.
        Map<String,Object> payload = Map.of("customer", Map.of("document", "DOC1", "name", "John", "lastName", "Doe"));
        List<PendingFunctionCall> calls = new Workflow(WF).extractFunctionCalls(payload, Map.of());
        String extractKey = calls.get(0).getKey();

        // Recompute via the same public key util with the same resolved args:
        String recomputed = com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey.of(
            "screening", Map.of("0", "DOC1", "1", "John Doe"));
        Assertions.assertEquals(recomputed, extractKey);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=FunctionCallExtractorTest test`
Expected: FAIL — `PendingFunctionCall` / `extractFunctionCalls` do not exist.

- [ ] **Step 3a: Create the PendingFunctionCall VO**

```java
package com.gatekeeperx.ruleflow.vo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A custom-function call site with its arguments already resolved against the payload. */
public class PendingFunctionCall {

    private final String functionName;
    private final Map<String, Object> args;
    private final String key;

    public PendingFunctionCall(String functionName, Map<String, Object> args, String key) {
        this.functionName = functionName;
        this.args = args != null ? new LinkedHashMap<>(args) : new LinkedHashMap<>();
        this.key = key;
    }

    public String getFunctionName() { return functionName; }

    public Map<String, Object> getArgs() { return Collections.unmodifiableMap(args); }

    public String getKey() { return key; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PendingFunctionCall that)) return false;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() { return Objects.hash(key); }

    @Override
    public String toString() {
        return "PendingFunctionCall{functionName='" + functionName + "', args=" + args + ", key='" + key + "'}";
    }
}
```

- [ ] **Step 3b: Extract a reusable buildArgs in CustomFunctionCallContextEvaluator**

Replace the inline args-building loop in `evaluate(...)` with a call to a new static method, and add the method (keeps extract and evaluate identical — DRY):

```java
    public static Map<String, Object> buildArgs(
            RuleFlowLanguageParser.CustomFunctionCallContext ctx, Visitor visitor)
            throws PropertyNotFoundException, UnexpectedSymbolException {
        Map<String, Object> args = new LinkedHashMap<>();
        int positionalIndex = 0;
        for (RuleFlowLanguageParser.FuncCallArgContext argCtx : ctx.funcCallArg()) {
            if (argCtx.argName != null) {
                args.put(argCtx.argName.getText(), visitor.visit(argCtx.argValue));
            } else {
                args.put(String.valueOf(positionalIndex++), visitor.visit(argCtx.argValue));
            }
        }
        return args;
    }
```

Then in `evaluate(...)`, replace the manual loop with:

```java
        Map<String, Object> args = buildArgs(ctx, visitor);
```

- [ ] **Step 3c: Create the FunctionCallExtractorVisitor**

```java
package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.evaluators.CustomFunctionCallContextEvaluator;
import com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey;
import com.gatekeeperx.ruleflow.vo.PendingFunctionCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks a workflow tree and, for every custom-function call site, evaluates its
 * arguments against the payload (using the same machinery as evaluation) to
 * produce a {@link PendingFunctionCall}. Does not invoke any function. Call
 * sites whose arguments cannot be resolved without invoking another function
 * (nested async) are skipped with a warning.
 */
public class FunctionCallExtractorVisitor extends com.gatekeeperx.ruleflow.RuleFlowLanguageBaseVisitor<Void> {

    private static final Logger logger = LoggerFactory.getLogger(FunctionCallExtractorVisitor.class);

    private final Visitor argEvaluator;
    private final Map<String, PendingFunctionCall> byKey = new LinkedHashMap<>();

    public FunctionCallExtractorVisitor(Map<String, ?> payload, Map<String, List<?>> lists) {
        // No functions map: any nested custom-function arg will throw and be skipped.
        this.argEvaluator = new Visitor(payload, lists != null ? lists : Map.of(), payload);
    }

    @Override
    public Void visitCustomFunctionCall(RuleFlowLanguageParser.CustomFunctionCallContext ctx) {
        String functionName = ctx.ID().getText();
        try {
            Map<String, Object> args = CustomFunctionCallContextEvaluator.buildArgs(ctx, argEvaluator);
            String key = RuleflowFunctionKey.of(functionName, args);
            byKey.putIfAbsent(key, new PendingFunctionCall(functionName, args, key));
        } catch (Exception e) {
            logger.warn("Skipping pre-resolution of '{}': arguments could not be resolved ({})",
                functionName, e.getMessage());
        }
        // Do not descend into the call's own arguments for further extraction.
        return null;
    }

    public List<PendingFunctionCall> getPendingCalls() {
        return new ArrayList<>(byKey.values());
    }
}
```

- [ ] **Step 3d: Add extractFunctionCalls to Workflow**

Add to `Workflow.java`:

```java
    public java.util.List<com.gatekeeperx.ruleflow.vo.PendingFunctionCall> extractFunctionCalls(
            Map<String, Object> payload, Map<String, List<?>> lists) {
        com.gatekeeperx.ruleflow.visitors.FunctionCallExtractorVisitor extractor =
            new com.gatekeeperx.ruleflow.visitors.FunctionCallExtractorVisitor(payload, lists);
        extractor.visit(tree);
        return extractor.getPendingCalls();
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=FunctionCallExtractorTest,CustomFunctionTest test`
Expected: PASS — new extractor tests pass AND existing `CustomFunctionTest` still passes (proves the buildArgs refactor is behavior-preserving).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gatekeeperx/ruleflow/vo/PendingFunctionCall.java \
        src/main/java/com/gatekeeperx/ruleflow/visitors/FunctionCallExtractorVisitor.java \
        src/main/java/com/gatekeeperx/ruleflow/evaluators/CustomFunctionCallContextEvaluator.java \
        src/main/java/com/gatekeeperx/ruleflow/Workflow.java \
        src/test/java/FunctionCallExtractorTest.java
git commit -m "feat: extract resolved custom-function call sites before evaluation"
```

---

### Task A3: Inject pre-resolved results at evaluation

**Files:**
- Modify: `src/main/java/com/gatekeeperx/ruleflow/visitors/Visitor.java` (resolvedFunctions field + getter + constructor)
- Modify: `src/main/java/com/gatekeeperx/ruleflow/visitors/RulesetVisitor.java` (thread resolvedFunctions through)
- Modify: `src/main/java/com/gatekeeperx/ruleflow/Workflow.java` (new evaluate overload)
- Modify: `src/main/java/com/gatekeeperx/ruleflow/evaluators/CustomFunctionCallContextEvaluator.java` (check resolvedFunctions first)
- Test: `src/test/java/FunctionPreResolutionTest.java`

**Interfaces:**
- Consumes: `RuleflowFunctionKey.of(...)` (A1), `buildArgs(...)` (A2).
- Produces:
  - `WorkflowResult Workflow.evaluate(Map<String,Object> payload, Map<String,List<?>> lists, Map<String,RuleflowFunction> functions, Map<String,Object> resolvedFunctions)`.
  - `Map<String,Object> Visitor.getResolvedFunctions()` (never null).

- [ ] **Step 1: Write the failing test**

```java
import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FunctionPreResolutionTest {

    private static final String WF = """
        workflow 'test'
            ruleset 'sanctions'
                'hit' screening(customer.document).matchCount > 0 return block
            default allow
        end
        """;

    @Test
    public void preResolvedHitSkipsFunctionCall() {
        AtomicInteger calls = new AtomicInteger();
        RuleflowFunction spy = args -> { calls.incrementAndGet(); return Map.of("matchCount", 0); };

        String key = RuleflowFunctionKey.of("screening", Map.of("0", "DOC1"));
        Map<String,Object> resolved = Map.of(key, Map.of("matchCount", 3));

        WorkflowResult result = new Workflow(WF).evaluate(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of(),
            Map.of("screening", spy),
            resolved);

        Assertions.assertEquals("block", result.getResult());   // used injected matchCount=3
        Assertions.assertEquals(0, calls.get());                 // function never invoked
    }

    @Test
    public void cacheMissFallsBackToFunction() {
        AtomicInteger calls = new AtomicInteger();
        RuleflowFunction spy = args -> { calls.incrementAndGet(); return Map.of("matchCount", 7); };

        WorkflowResult result = new Workflow(WF).evaluate(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of(),
            Map.of("screening", spy),
            Map.of()); // empty resolved map -> miss

        Assertions.assertEquals("block", result.getResult());   // used function matchCount=7
        Assertions.assertEquals(1, calls.get());                 // fell back to calling
    }

    @Test
    public void existingThreeArgOverloadUnaffected() {
        RuleflowFunction fn = args -> Map.of("matchCount", 0);
        WorkflowResult result = new Workflow(WF).evaluate(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of(),
            Map.of("screening", fn));
        Assertions.assertEquals("allow", result.getResult());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=FunctionPreResolutionTest test`
Expected: FAIL — the 4-arg `evaluate` overload does not exist.

- [ ] **Step 3a: Add resolvedFunctions to Visitor**

In `Visitor.java` add the field and getter, and a constructor that accepts it. Keep existing constructors delegating with an empty map:

```java
    private final Map<String, Object> resolvedFunctions;
```

Update constructors:

```java
    public Visitor(Map<String, ?> data, Map<String, List<?>> lists, Map<String, ?> root) {
        this(data, lists, root, Map.of(), Map.of());
    }

    public Visitor(Map<String, ?> data, Map<String, List<?>> lists, Map<String, ?> root,
                   Map<String, RuleflowFunction> functions) {
        this(data, lists, root, functions, Map.of());
    }

    public Visitor(Map<String, ?> data, Map<String, List<?>> lists, Map<String, ?> root,
                   Map<String, RuleflowFunction> functions, Map<String, Object> resolvedFunctions) {
        this.data = data;
        this.lists = lists != null ? lists : Map.of();
        this.root = root;
        this.functions = functions != null ? functions : Map.of();
        this.resolvedFunctions = resolvedFunctions != null ? resolvedFunctions : Map.of();
    }
```

Add getter near `getFunctions()`:

```java
    public Map<String, Object> getResolvedFunctions() {
        return resolvedFunctions;
    }
```

- [ ] **Step 3b: Thread resolvedFunctions through RulesetVisitor**

In `RulesetVisitor.java` add a field and a constructor overload, and pass it when building the `Visitor` in `visitWorkflow`:

```java
    private final Map<String, Object> resolvedFunctions;

    public RulesetVisitor(Map<String, ?> data, Map<String, List<?>> lists) {
        this(data, lists, Map.of(), Map.of());
    }

    public RulesetVisitor(Map<String, ?> data, Map<String, List<?>> lists,
                          Map<String, RuleflowFunction> functions) {
        this(data, lists, functions, Map.of());
    }

    public RulesetVisitor(Map<String, ?> data, Map<String, List<?>> lists,
                          Map<String, RuleflowFunction> functions,
                          Map<String, Object> resolvedFunctions) {
        this.data = data;
        this.lists = lists;
        this.functions = functions != null ? functions : Map.of();
        this.resolvedFunctions = resolvedFunctions != null ? resolvedFunctions : Map.of();
    }
```

In `visitWorkflow`, change the visitor construction (line ~49):

```java
        Visitor visitor = new Visitor(data, lists, data, functions, resolvedFunctions);
```

- [ ] **Step 3c: Add the evaluate overload to Workflow**

```java
    public WorkflowResult evaluate(Map<String, Object> request,
                                   Map<String, List<?>> lists,
                                   Map<String, RuleflowFunction> functions,
                                   Map<String, Object> resolvedFunctions) {
        return new RulesetVisitor(request, lists, functions, resolvedFunctions).visit(tree);
    }
```

- [ ] **Step 3d: Check resolvedFunctions first in the evaluator**

In `CustomFunctionCallContextEvaluator.evaluate(...)`, after building args, before the existing `functionCallCache` logic:

```java
        Map<String, Object> args = buildArgs(ctx, visitor);

        String resolvedKey = com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey.of(functionName, args);
        Map<String, Object> resolvedFunctions = visitor.getResolvedFunctions();
        if (resolvedFunctions.containsKey(resolvedKey)) {
            return resolvedFunctions.get(resolvedKey);
        }
```

Note: the `function == null` check must move to AFTER this block, so a pre-resolved hit works even when no live function implementation is supplied. Reorder so the null-check throws only on a miss:

```java
        // (resolved-hit check above returns early)
        if (function == null) {
            throw new UnexpectedSymbolException("Custom function '" + functionName + "' is not defined");
        }
        // ... existing List-key cache + function.apply ...
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=FunctionPreResolutionTest,CustomFunctionTest test`
Expected: PASS — new injection tests pass AND existing `CustomFunctionTest` unaffected.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gatekeeperx/ruleflow/visitors/Visitor.java \
        src/main/java/com/gatekeeperx/ruleflow/visitors/RulesetVisitor.java \
        src/main/java/com/gatekeeperx/ruleflow/Workflow.java \
        src/main/java/com/gatekeeperx/ruleflow/evaluators/CustomFunctionCallContextEvaluator.java \
        src/test/java/FunctionPreResolutionTest.java
git commit -m "feat: inject pre-resolved function results into evaluation"
```

---

### Task A4: Version bump, changelog, full build

**Files:**
- Modify: `pom.xml` (version)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Bump version**

In `pom.xml`, change the project `<version>0.18.0</version>` to `<version>0.19.0</version>`.

- [ ] **Step 2: Add changelog entry**

Prepend an entry to `CHANGELOG.md`:

```markdown
## 0.19.0
- Add async pre-resolution of custom functions: `Workflow.extractFunctionCalls(payload, lists)` returns resolved call sites keyed by a canonical hash, and `Workflow.evaluate(payload, lists, functions, resolvedFunctions)` injects pre-resolved results so a slow function (e.g. `screening`) is not called synchronously when its result was pre-computed. Fully backward compatible.
```

- [ ] **Step 3: Run the full test suite**

Run: `mvn -q test`
Expected: PASS — entire suite green (no regressions).

- [ ] **Step 4: Build and install locally (so rules-engine can consume 0.19.0)**

Run: `mvn -q -DskipTests install`
Expected: BUILD SUCCESS; `~/.m2/repository/com/gatekeeperx/ruleflow/0.19.0/` populated.

- [ ] **Step 5: Commit**

```bash
git add pom.xml CHANGELOG.md
git commit -m "chore: bump ruleflow to 0.19.0"
```

---

## Self-Review

**Spec coverage:**
- §3.1 canonical key → Task A1. ✓
- §4.1 key utility → A1. ✓
- §4.2 extract → A2. ✓
- §4.3 inject overload + evaluator lookup → A3. ✓
- §4.4 miss policy (fallback) → A3 Step 3d + `cacheMissFallsBackToFunction` test. ✓
- §4.5 version bump → A4. ✓

**Placeholder scan:** No TBD/TODO; all steps contain concrete code. ✓

**Type consistency:** `RuleflowFunctionKey.of(String, Map<String,Object>)`, `PendingFunctionCall(String, Map<String,Object>, String)`, `buildArgs(CustomFunctionCallContext, Visitor)`, `extractFunctionCalls(Map<String,Object>, Map<String,List<?>>)`, `evaluate(..., Map<String,Object> resolvedFunctions)`, `Visitor.getResolvedFunctions()` — consistent across tasks. ✓

# Async Pre-Resolution of Custom Functions (screening) — Design

Date: 2026-08-27
Repos: `ruleflow-java`, `rules-engine`
Status: Approved design, pending spec review

## 1. Problem

RuleFlow workflows can call custom functions from the DSL, e.g.:

```
workflow 'test_screening'
    ruleset 'sanctions'
        'hit' screening(customer.document, customer.name + ' ' + customer.lastName).matchCount > 0 return block
    default allow
end
```

Today `screening()` is resolved **synchronously during `.evaluate()`**. Its
implementation (`RuleflowFunctionRegistry.screening` in rules-engine) performs a
slow external Sanctions call. For high-volume **offline** evaluations (millions
of events), doing this call inline during evaluation is too slow and risks
saturating memory/threads.

We need a way to **pre-resolve** these functions asynchronously *before*
evaluation, so that by the time evaluation reaches `screening(...)` the result is
already available and no synchronous external call happens.

## 2. Goals / Non-Goals

### Goals
- Resolve slow custom functions asynchronously, before evaluation.
- Keep the DSL identical: `screening(...)` keeps the same name and signature; no
  existing rules are rewritten.
- Make the mechanism **generic** — any custom function (not just `screening`)
  can be pre-resolved through the same path.
- Eliminate argument-formatting mismatches (e.g. `name + ' ' + lastName`) by
  resolving arguments through the exact same evaluation machinery both when
  pre-resolving and when looking up.
- Backward compatible: existing `Workflow.evaluate(...)` overloads and the
  rules-engine **online** decision path are untouched.

### Non-Goals
- Changing the online/real-time `DecisionService` flow. This work adds a **new**
  offline flow in rules-engine; the current flow stays as-is.
- Pre-resolving nested async calls, e.g. `f(screening(x))` where `f` is also
  async. Out of scope for v1 (screening arguments are plain payload expressions).
- Choosing/queueing infrastructure changes beyond reusing the existing RabbitMQ
  tenant-aware listener pattern.

## 3. Core Mechanism

The existing evaluator already contains the exact injection point.
`CustomFunctionCallContextEvaluator`:

```java
// args are already evaluated from the payload (concatenations resolved)
List<Object> cacheKey = [functionName, args];
if (cache.containsKey(cacheKey)) return cache.get(cacheKey);  // function NEVER called
Object result = function.apply(args);                          // only on a miss
```

Pre-resolution = **compute that key ahead of time, resolve the result
asynchronously, and hand the result map into `.evaluate()`** so the lookup hits
and `function.apply` is never reached.

### 3.1 Canonical key (shared identity)

A single deterministic, serializable string key identifies a resolved call, used
identically on both sides of the async boundary (the "unique id / hash" from the
design discussion):

```
key = canonicalKey(functionName, resolvedArgs)
```

- `resolvedArgs` is the argument map **after** evaluation against the payload
  (so `name + ' ' + lastName` is already the final string).
- Serialization is deterministic: argument keys sorted, stable value formatting.
- Unlike today's in-memory `List<Object>` key, this string key is serializable
  for storage and replay.

Implementation lives in ruleflow-java so both the extract and the evaluator
compute it identically (single source of truth).

## 4. ruleflow-java Changes (additive, backward compatible)

### 4.1 Canonical key utility
New helper (e.g. `RuleflowFunctionKey.of(String functionName, Map<String,Object> resolvedArgs) -> String`)
producing the deterministic string described in 3.1. Used by both the extractor
and `CustomFunctionCallContextEvaluator`.

### 4.2 Extract: pending function calls
New capability on `Workflow`:

```java
List<PendingFunctionCall> extractFunctionCalls(Map<String,Object> payload,
                                               Map<String, List<?>> lists);
```

- Walks the parse tree; for every `customFunctionCall` node it **evaluates the
  argument expressions against the payload** using the existing `Visitor`
  machinery (identical resolution to eval time), builds `resolvedArgs`, computes
  the canonical `key`, and records a `PendingFunctionCall { functionName,
  resolvedArgs, key }`.
- Deduplicates by `key`.
- Does **not** invoke any function (no functions map required).
- Emits a warning (and skips) when it encounters a nested custom-function call as
  an argument (v1 out-of-scope case).

`PendingFunctionCall` is a new value object in `ruleflow.vo`.

Implementation note: a dedicated `FunctionCallExtractorVisitor` descends the
tree and delegates argument evaluation to a `Visitor` seeded with the payload —
mirroring how `CustomFunctionCallContextEvaluator` builds its args map today.

### 4.3 Inject: pre-resolved results
New `evaluate` overload on `Workflow`, threaded through
`RulesetVisitor` → `Visitor`:

```java
WorkflowResult evaluate(Map<String,Object> payload,
                        Map<String, List<?>> lists,
                        Map<String, RuleflowFunction> functions,
                        Map<String, Object> resolvedFunctions);  // key -> result
```

`Visitor` holds the `resolvedFunctions` map (keyed by canonical string key).
`CustomFunctionCallContextEvaluator` becomes:

1. Build `args` (as today).
2. Compute `key = canonicalKey(functionName, args)`.
3. If `resolvedFunctions` contains `key` → return it. **Function never called.**
4. Else fall back to existing behavior (in-memory per-eval cache, then
   `function.apply`).

All existing overloads delegate with an empty `resolvedFunctions`, so behavior
is unchanged when the feature is unused.

### 4.4 Miss policy: kept in the engine, not ruleflow
ruleflow always falls back to calling the function on a miss (step 4 above).
Whether a miss is a real (slow) call or a guarded no-op is decided by **which
functions map the engine passes** for a given tenant/flow — ruleflow stays
policy-free. (Rationale: the tenant flag and SLA policy live in rules-engine.)

### 4.5 Versioning
Minor bump `0.18.0 -> 0.19.0`. Purely additive; no breaking changes.

## 5. rules-engine Changes (NEW offline flow only)

**The online path — `DecisionService.decide()` and everything it calls — is not
modified.** All changes below are a separate offline evaluation flow.

Existing infra reused: RabbitMQ via `spring-boot-starter-amqp` /
`multi-tenant-enabler-rabbitmq`, with the `TenantAwareRabbitListener` +
`MessageListener` pattern already used by `EventsListener`.

Chained-queue pipeline (per the design discussion):

```
[offline event] -> enrichment consumer -> async-prevaluation consumer -> offline evaluation
```

### 5.1 Async-prevaluation consumer (new)
A new tenant-aware Rabbit listener ("async prevaluation" queue):
1. Loads the workflow definition for the event.
2. Calls `workflow.extractFunctionCalls(payload, lists)`.
3. Dispatches each `PendingFunctionCall` **by function name** to a registered
   resolver. First resolver: `screening` -> `SanctionsListsService` (reusing the
   existing request/response mapping in `RuleflowFunctionRegistry`, refactored so
   the mapping logic is shared, not duplicated).
4. Applies a **60s timeout** per resolution.
5. Stores the resulting `{ key -> result }` map on the offline event/record so
   the evaluation step (and any replay) can read it back.

The dispatch-by-name design is what makes it generic: future async functions
register a resolver and need no evaluator changes.

### 5.2 Offline evaluation step (new)
Loads the stored `{ key -> result }` map and calls the new overload:

```java
workflow.evaluate(payload, lists, functions, resolvedFunctions);
```

`functions` still contains the real `screening` implementation, so a **miss**
(prevaluation incomplete / not run) degrades to the current inline behavior
rather than failing — safe migration. A metric/warning is emitted on miss so
misconfiguration is visible rather than silent.

### 5.3 Per-tenant flag
The offline screening workflow is gated by a per-tenant flag so it runs only
where configured (from the design discussion). Off = the new flow is inert for
that tenant.

### 5.4 Storage / replay
The `{ key -> result }` map is persisted with the offline event record (aligned
with how features/clickhouse results are already persisted for the online flow),
so offline replays are deterministic and reuse resolved results.

## 6. Backward Compatibility Summary
- ruleflow-java: new key util, new `extractFunctionCalls`, new 4-arg `evaluate`,
  new `PendingFunctionCall` VO. All existing APIs delegate with empty
  `resolvedFunctions`. No behavior change for existing callers.
- rules-engine: online decision flow unchanged. All new code is the offline
  pipeline. `screening` keeps its name/signature and remains the fallback.

## 7. Testing Strategy
- **ruleflow-java**
  - Canonical key: determinism, arg-order independence, value formatting,
    equality between extract-time and eval-time keys for the same resolved args.
  - `extractFunctionCalls`: resolves concatenated args (`name + ' ' + lastName`),
    dedups repeated identical calls, emits warning on nested async arg, returns
    empty for workflows with no custom functions.
  - Injection: a hit returns the injected result and the function is never
    invoked (assert via a spy function that throws if called); a miss falls back
    to the function; existing overloads behave exactly as before.
- **rules-engine**
  - Prevaluation consumer: extract -> resolve -> store, with a mocked
    `SanctionsListsService`; 60s timeout path; dispatch-by-name.
  - Offline evaluation: hit path performs zero Sanctions calls; miss path warns
    + falls back; per-tenant flag gating.
  - Explicit regression assertion that the online `DecisionService` path is
    unchanged (no new pre-resolution behavior leaks into it).

## 8. Assumptions
- A1 — Miss policy lives in rules-engine (ruleflow falls back to calling).
- A2 — Resolved results persist with the offline event record for replay.
- A3 — Nested async calls out of scope for v1 (warn + skip).
- A4 — ruleflow ships as additive minor bump 0.19.0.
- A5 — Offline flow is RabbitMQ, reusing the tenant-aware listener pattern; exact
  queue names/bindings finalized during implementation planning.

## 9. Open Questions (to resolve during planning)
- Exact persistence location/schema for the `{key -> result}` map in the offline
  record.
- Where the shared Sanctions request/response mapping should live once extracted
  from `RuleflowFunctionRegistry` (shared service vs. mapper).
- Enrichment consumer: new or existing? (Assumed a prior stage exists; this spec
  only requires that payload fields needed for screening args are present before
  prevaluation runs.)

# RuleFlow DSL — Complete Reference

RuleFlow is a declarative rule engine DSL for expressing business logic as readable, structured workflows. Rules evaluate against a data payload and produce a named result, optional actions, and optional computed variables.

---

## Table of Contents

1. [Quick Start](#1-quick-start)
2. [Workflow Structure](#2-workflow-structure)
3. [Rules and Results](#3-rules-and-results)
4. [Expressions and Conditions](#4-expressions-and-conditions)
5. [Property Access](#5-property-access)
6. [Literals and Values](#6-literals-and-values)
7. [Math Operators](#7-math-operators)
8. [List & Collection Operations](#8-list--collection-operations)
9. [set — Variable Assignment](#9-set--variable-assignment)
10. [continue — Score and Proceed](#10-continue--score-and-proceed)
11. [Actions](#11-actions)
12. [Custom Functions](#12-custom-functions)
13. [Date and Time](#13-date-and-time)
14. [String Similarity](#14-string-similarity)
15. [Geo Operations](#15-geo-operations)
16. [Regex](#16-regex)
17. [Evaluation Modes](#17-evaluation-modes)
18. [Error Handling and Warnings](#18-error-handling-and-warnings)
19. [Case Sensitivity](#19-case-sensitivity)
20. [Operator Precedence](#20-operator-precedence)
21. [Full Examples](#21-full-examples)
22. [Best Practices](#22-best-practices)
23. [Reserved Keywords](#23-reserved-keywords)

---

## 1. Quick Start

```
workflow 'fraud_detection'
    ruleset 'basic_checks'
        'high_amount'     amount > 1000        return review with manual_check
        'blocked_country' country in 'XX', 'YY' return block
    default allow
end
```

### Java API

```java
import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;

Workflow wf = new Workflow(workflowString);
WorkflowResult result = wf.evaluate(
    Map.of("amount", 1500, "country", "US"),
    Map.of(),                                  // named lists (optional)
    Map.of("myFn", myRuleflowFunction)         // custom functions (optional)
);

String decision      = result.getResult();
Set<String> warnings = result.getWarnings();
List<Action> actions = result.getActionCalls();
Map<String, Object> vars = result.getVariables();
```

---

## 2. Workflow Structure

```
WORKFLOW '<name>'
    [EVALUATION_MODE SINGLE_MATCH | MULTI_MATCH]

    RULESET '<name>' [<condition> THEN]
        '<rule_name>' [<condition>] [set ...] (RETURN <result> | THEN <actions> | CONTINUE) [WITH <actions>]
        ...

    DEFAULT [RETURN] <result> [WITH <actions>]
END
```

All keywords (`WORKFLOW`, `RULESET`, `RETURN`, `THEN`, `DEFAULT`, `END`, `SET`, `CONTINUE`, etc.) are **case-insensitive**.

### Ruleset condition (guard)

A ruleset can be guarded by an optional condition. If the condition is false, the entire ruleset is skipped.

```
RULESET 'high_value_checks' amount > 10000 THEN
    'rule_a' ...
    'rule_b' ...
```

### Default clause

The `DEFAULT` clause is required. It fires when no rule returns a result.

```
DEFAULT RETURN 'approved'
DEFAULT allow          -- RETURN keyword is optional
DEFAULT THEN allow     -- THEN keyword also accepted
```

### Comments

```
-- single-line comment
/* multi-line
   comment */
```

---

## 3. Rules and Results

Each rule has a name (single-quoted), an optional condition, and a result clause.

### RETURN — named result

```
'check_amount' amount > 1000 RETURN 'block'
'check_amount' amount > 1000 RETURN block   -- quotes optional for simple identifiers
```

### Always-true rule (no condition)

Omit the condition for a rule that always fires:

```
'fallback'  RETURN low_risk                        -- always matches
'label'     set $tier = 'standard' RETURN labelled -- set + always-true
```

Useful as a guaranteed catch-all inside a guarded ruleset, or as a labelling step before `continue`.

### RETURN with no explicit result

When `RETURN` appears without a result value, the rule name is used:

```
'approved' amount < 1000 RETURN   -- result is "approved" (rule name)
```

### THEN — fire actions and optionally continue

`THEN` fires one or more actions. Without `CONTINUE` the rule stops (result = rule name); with `CONTINUE` actions are accumulated and evaluation proceeds:

```
-- THEN without CONTINUE: stops evaluation, result = rule name
'flag_account' score > 80 THEN action('send_alert')

-- THEN + CONTINUE: accumulates actions and keeps evaluating
'tag_high_value' amount > 1000 THEN flag_for_review CONTINUE
'tag_and_score'  amount > 500  THEN action('tag', {'type': 'vip'}) AND log_event CONTINUE
```

Actions from `THEN … CONTINUE` rules are merged into the final result of whichever `RETURN` rule or `DEFAULT` eventually fires.

### Inline actions + CONTINUE (no THEN)

Actions and `CONTINUE` can be written directly after any `set` clauses, without `THEN`:

```
'<rule_name>' <condition>
    set $score = expr
    action('block') CONTINUE

'<rule_name>' <condition>
    action('flag') AND action('log') CONTINUE
```

### RETURN EXPR — dynamic result

Use `EXPR(...)` when the result should be computed from an expression:

```
'calc_price' discount > 0 RETURN EXPR(amount * (1 - discount))
```

---

## 4. Expressions and Conditions

### Comparison operators

| Operator | Meaning | Notes |
|---|---|---|
| `=` | Equal (case-insensitive) | Ignores case for string comparisons |
| `==` | Equal (case-sensitive) | Exact string match |
| `<>` | Not equal | Works with strings, numbers, and `null` |
| `<` | Less than | |
| `<=` | Less than or equal | |
| `>` | Greater than | |
| `>=` | Greater than or equal | |

```
country = 'us'     -- matches 'US', 'us', 'Us', etc.
country == 'US'    -- only matches exactly 'US'
amount <> 0
score >= 750
```

### Boolean operators

```
amount > 100 AND country = 'US'
status = 'active' OR status = 'pending'
NOT (status = 'blocked')
```

`AND` has higher precedence than `OR`. Use parentheses to override:

```
(status = 'active' OR status = 'pending') AND amount > 50
```

### Null checks

```
'null_check' field = null  RETURN 'missing_field'
'not_null'   field <> null RETURN 'has_value'
```

---

## 5. Property Access

Properties are read from the data payload passed at evaluation time.

### Simple and nested properties

```
amount
userId
user.email
order.address.city
device.info.fingerprint
```

Each segment must be present. If any is missing, the rule is skipped with a warning.

### Leading dot — root context

A leading `.` forces resolution from the root data context, escaping the local scope in aggregation predicates or `evalInList`:

```
items.any { type = .requestedType }    -- .requestedType reads from root, not item
```

### elem — list item reference in evalInList

`elem` refers to the current item when iterating inside `evalInList`:

```
evalInList('users', elem.status = 'active')
evalInList('orders', elem.amount > 100 AND elem.country = userId.country)
```

### it — current-item reference in aggregation predicates

`it` refers to the current item inside `.any`, `.all`, `.none`, `.contains` predicates. Use `it.field` to access a field on the current item explicitly — useful when the field name collides with a root-level property or for clarity:

```
matches.contains { it.matchPriority > 3 }
items.any { it.price > 100 AND it.category = 'electronics' }
```

`it.field` and bare `field` are both valid inside aggregation predicates — `field` first looks in the current item, then falls back to the parent context; `it.field` always refers to the current item.

---

## 6. Literals and Values

### Strings

Single-quoted strings:

```
'approved'
'US'
'high-risk'
```

### Numbers

```
1000
3.14
1.5e3   -- scientific notation (= 1500)
-42
```

### Booleans

```
true
false
```

### Null

```
null
```

### Current date

```
currentDate()   -- returns today's date at runtime
```

---

## 7. Math Operators

| Operator | Meaning |
|---|---|
| `+` | Addition (numeric) or string concatenation |
| `-` | Subtraction |
| `*` | Multiplication |
| `/` | Division |
| `%` or `mod` | Modulo (remainder) |
| `abs(expr)` | Absolute value |

### String concatenation

When either operand of `+` is a non-numeric string, the operator concatenates:

```
firstName + ' ' + lastName == 'John Doe'
'TXN-' + transactionId
```

Only `+` has this dual behaviour; `-` is always numeric.

```
'markup'   amount * 1.2 > 500                       RETURN 'expensive'
'even'     count % 2 = 0                             RETURN 'even'
'distance' abs(delta) > 10                           RETURN 'outlier'
'combined' (base + bonus) * multiplier > threshold   RETURN 'qualified'
'name'     firstName + ' ' + lastName == 'Jane Doe'  RETURN 'matched'
```

---

## 8. List & Collection Operations

This section consolidates all list and collection operations, grouped by intent.

---

### 1. Check if a value is IN a list (membership)

| Syntax | Description |
|---|---|
| `expr IN 'a', 'b', 'c'` | Exact match against inline literals |
| `expr NOT IN 'a', 'b'` | Negated exact match |
| `expr IN list('name')` | Exact match against named/stored list |
| `expr NOT IN list('name')` | Negated match against stored list |
| `(f1, f2) IN list('name')` | Tuple match (multi-field) against stored list |
| `(f1, f2) NOT IN list('name')` | Negated tuple match |
| `(f1, f2) IN (('a','b'), ('c','d'))` | Tuple match against inline tuple literal |

```
country IN 'US', 'CA', 'GB'
status NOT IN 'blocked', 'suspended'
country IN list('allowed_countries')
device.fingerprint NOT IN list('blacklisted_devices')
(device.fingerprint, user.ip) IN list('blocked_combos')
(country, currency) IN ('US', 'USD'), ('GB', 'GBP')
```

All fields in a tuple must match. Named lists are provided at runtime via the `lists` parameter of `workflow.evaluate(...)`.

---

### 2. String membership (CONTAINS, STARTS_WITH)

`CONTAINS` and `STARTS_WITH` check whether a string value contains or starts with any element in the list. These are **not** the same as the `.contains{}` aggregation operator.

| Syntax | Description |
|---|---|
| `expr CONTAINS 'sub1', 'sub2'` | Value contains any of the substrings |
| `expr NOT CONTAINS 'sub'` | Negated substring check |
| `expr CONTAINS list('name')` | Value contains any element of stored list |
| `expr STARTS_WITH 'pfx1', 'pfx2'` | Value starts with any prefix |
| `expr NOT STARTS_WITH 'pfx'` | Value starts with none of the prefixes |
| `expr STARTS_WITH list('name')` | Value starts with any element of stored list |

```
email CONTAINS '@gmail.com', '@yahoo.com'
description NOT CONTAINS 'spam', 'phishing'
country_code STARTS_WITH 'US', 'CA'
```

---

### 3. Aggregations on a field (list nested in request data)

Aggregations operate on list-type fields within the evaluated data payload.

```
<field>.<operation> { <predicate> }
<field>.<operation> ()
```

| Syntax | Description | Return type |
|---|---|---|
| `field.any { predicate }` | True if at least one item matches | Boolean |
| `field.contains { predicate }` | Alias for `.any{}` | Boolean |
| `field.all { predicate }` | True if every item matches | Boolean |
| `field.none { predicate }` | True if no item matches | Boolean |
| `field.count { predicate }` | Count of items matching predicate | Number |
| `field.count()` | Total item count | Number |
| `field.average { predicate }` | Average value across matched items | Number |
| `field.average()` | Average of all items | Number |
| `field.distinct { predicate }` | Distinct values from matched items | Collection |
| `field.distinct()` | All distinct values | Collection |

Inside `{ }`, `it` refers to the current item. Bare field names also resolve against the current item first, then fall back to parent context.

```
-- Does any item equal the string 'fraud'?
tags.any { 'fraud' }

-- Does any item have type = 'restricted'?
items.any { type = 'restricted' }

-- Use it.field for explicit current-item access
matches.contains { it.matchPriority > 3 }

-- Are all transactions above 10?
transactions.all { amount > 10 }

-- How many orders have status = 'pending'?
orders.count { status = 'pending' } >= 2

-- Average price of premium items
items.average { category = 'premium' } > 500
```

**Null-safe evaluation:** if a predicate field is `null`, that item is treated as not matching and evaluation continues silently.

```
-- items with null priority are skipped; only items where priority > 3 match
items.contains { it.priority > 3 }
```

---

### 4. Aggregations on a stored/named list

The same aggregation operators work on named lists provided at runtime, using `list('name')` as the target.

| Syntax | Description |
|---|---|
| `list('name').any { predicate }` | Any item in named list matches predicate |
| `list('name').contains { predicate }` | Alias for `.any{}` |
| `list('name').all { predicate }` | All items match |
| `list('name').none { predicate }` | No items match |
| `list('name').count { predicate }` | Count matching items |
| `list('name').count()` | Total items in list |
| `list('name').average { predicate }` | Average across matched items |
| `list('name').distinct { predicate }` | Distinct values from matched items |

Inside predicates, `it` refers to the current list item. Parent-context properties (request-level fields) are accessible directly by name.

```
list('blacklist').any { it.userId = userId }
list('patterns').any { it.type = transaction.type AND it.region = user.region }
list('sessions').none { it.deviceId = device.id AND date(it.expiresAt) > date(now()) }
list('allowed_countries').count() > 0
```

---

### 5. evalInList (legacy / advanced)

`evalInList` is the original form for testing named lists. It is equivalent to `list('name').any { ... }`.

| Syntax | Description |
|---|---|
| `evalInList('name', elem.field = value)` | True if any item matches predicate |
| `evalInList('name', it.field = value)` | Same, using `it` alias for `elem` |

```
evalInList('blacklist', elem.userId = userId)
evalInList('patterns', elem.type = transaction.type AND elem.region = user.region)
evalInList('sessions', elem.deviceId = device.id AND date(elem.expiresAt) > date(now()))
```

You can reference request-level properties alongside `elem.*`. Prefer the `list('name').any { }` form for new rules — `evalInList` is retained for backwards compatibility.

---

## 9. set — Variable Assignment

`set` clauses compute and store intermediate values when a rule fires. Variables are available to all subsequent rules and rulesets within the same evaluation.

### Simple assignment

Variable names use a `$` prefix in both declarations and references:

```
'<rule_name>' <condition>
    set $variable = <expression>
    set $variable2 = <expression2>
    RETURN <result>
```

- Multiple `set` clauses execute top-to-bottom
- Later clauses can reference variables set earlier in the same rule
- Variables are workflow-scoped: visible in all subsequent rules and rulesets

### Compound assignment operators

Update a variable in-place. If the variable has not been set yet, its current value is treated as `0`.

| Operator | Meaning |
|---|---|
| `$var += expr` | `$var = $var + expr` |
| `$var -= expr` | `$var = $var - expr` |
| `$var *= expr` | `$var = $var * expr` |
| `$var /= expr` | `$var = $var / expr` |
| `$var %= expr` | `$var = $var % expr` |

The right-hand side can be any expression — literals, request properties, arithmetic, function calls:

```
set $score += amount * riskMultiplier
set $price -= amount * discount / 100
set $total *= 1.2
```

### Storing boolean expressions

The right-hand side can be a boolean expression. The result is stored as a `Boolean`:

```
set $isHigh = amount > 100    -- stores true or false

-- use it as a condition in a later rule:
'high' $isHigh return high_risk
```

### Accessing set variables

Use `$` prefix anywhere an expression is valid:

```
workflow 'pricing'
    ruleset 'scoring'
        'compute' amount > 0
            set $base_score = amount * riskMultiplier
            RETURN 'scored'

    ruleset 'decision'
        'block_high' $base_score > 5000 RETURN 'block'

    DEFAULT RETURN 'allow'
END
```

**Namespacing:** bare identifiers (e.g. `amount`) always read request data; `$`-prefixed identifiers (e.g. `$amount`) always read set variables. There is no ambiguity or shadowing.

### Member access on variable values

If a set variable holds an object (map), you can access its fields:

```
set $result = screening(userId)
$result.risk_score > 500
```

### Reading variables from the result

```java
Map<String, Object> vars = result.getVariables();
Object score = vars.get("base_score");   // key is bare name, no $
```

---

## 10. continue — Score and Proceed

`continue` lets a rule execute its `set` clauses (and optionally accumulate actions) without returning a final result. Evaluation proceeds to the next rule and subsequent rulesets.

### Plain continue

```
'<rule_name>' <condition>
    [set $variable = <expression> ...]
    CONTINUE
```

### THEN … CONTINUE — accumulate actions and keep evaluating

```
'<rule_name>' <condition>
    THEN action('some_action') [AND action('another')]
    CONTINUE
```

### Inline actions + CONTINUE (no THEN)

Actions can precede `CONTINUE` directly, without `THEN`:

```
'<rule_name>' <condition>
    set $score = expr
    action('block') CONTINUE

'<rule_name>' <condition>
    action('flag') AND action('log') CONTINUE
```

Actions accumulated from any `CONTINUE` rule are merged into the final result — whether that comes from a later `RETURN` rule or the `DEFAULT` clause.

### Example: multi-step AML scoring

```
workflow 'aml_onboarding'
    ruleset 'work_info'
        'employed'   workInfo.code = 1010 set $score = 0.45 * 10 continue
        'self_emp'   workInfo.code = 230  set $score += 0.45 * 5 continue

    ruleset 'sanctions'
        'hit'    screening(docNumber, fullName).matchCount > 5
                 set $score = 0.55 * 15
                 action('block') continue
        'hit_2'  screening(docNumber, fullName).matchCount > 0
                 set $score += 0.45 * 15
                 action('block') continue

    ruleset 'risk_rating'
        'low'    $score > 3  AND $score < 15 return low
        'medium' $score >= 15 AND $score < 30 return medium
        'high'   $score >= 30 return high

    default low
end
```

### Falling to default

If no subsequent rule matches, the `default` clause fires. Variables set by `continue` rules are available in `getVariables()` even when the default is returned.

---

## 11. Actions

Actions describe side effects to execute when a rule fires. They are returned in the workflow result for the caller to process.

### Built-in action syntax

```
action('<name>')
action('<name>', { '<param>': <value>, '<param2>': <value2> })
```

### Named action (shorthand)

```
manual_review
send_alert
apply_restriction({ 'level': 'high' })
```

### Chaining actions

Use `WITH` or `AND` to chain multiple actions:

```
RETURN 'block'
    WITH action('notify', {'channel': 'email'})
    AND  action('log_event', {'severity': 'high'})
    AND  manual_review
```

### Action parameter values

Parameter values can be string literals or request properties:

```
action('review', { 'user': userId, 'amount': amount, 'reason': 'velocity' })
```

### Reading actions from the result

```java
List<Action> actions = result.getActionCalls();
for (Action a : actions) {
    String name              = a.getName();
    Map<String, String> params = a.getParams();
}

// or as a set of names:
Set<String> actionNames = result.getActions();
```

---

## 12. Custom Functions

Custom functions inject external logic — API calls, ML scores, database lookups — into rule conditions.

### Calling a custom function

```
functionName(arg1, arg2, ...)        -- positional arguments
functionName()                       -- no-arg
functionName(name: expr, ...)        -- named arguments
```

Arguments can be any expression: properties, literals, arithmetic, string concatenation, or nested function calls.

```
'screen'   screening(userId) = 'pass'                                          RETURN 'approved'
'score'    riskScore(userId, country, amount) > 700                            RETURN 'review'
'flag'     isBlocked(deviceId)                                                 RETURN 'blocked'
'kyc'      kyc(documentNumber: docId, fullName: firstName + ' ' + lastName) == 'pass' RETURN 'approved'
```

### Member access on function return value

If a function returns an object (map), access its fields directly:

```
screening(userId).risk_score > 500
screening(userId).details.level == 'critical'
screening(userId).tags.any { 'fraud' }
screening(userId).matches.contains { it.matchPriority > 3 }
```

### Registering functions (Java API)

`RuleflowFunction` receives a `Map<String, Object>`. Positional args use keys `"0"`, `"1"`, …; named args use their declared names:

```java
RuleflowFunction screeningFn = args -> {
    String userId = (String) args.get("0");        // positional
    return Map.of("risk_score", 750, "tags", List.of("fraud"));
};

RuleflowFunction kycFn = args -> {
    String docNumber = (String) args.get("documentNumber");  // named
    String fullName  = (String) args.get("fullName");         // named
    return verifyKyc(docNumber, fullName);
};

workflow.evaluate(data, lists, Map.of("screening", screeningFn, "kyc", kycFn));
```

### Resilience

- **Undefined function** — rule skipped with a warning; `isError()` stays `false`
- **Function throws** — rule skipped with a warning containing the exception message; `isError()` stays `false`
- **Memoization** — each `(functionName, args)` combination is computed at most once per evaluation

---

## 13. Date and Time

### Getting the current date/time

```
now()            -- current ZonedDateTime
currentDate()    -- current date (alias)
```

### Parsing dates

```
date('2024-01-15')                 -- parse ISO date string (yyyy-MM-dd)
date(propertyName)                 -- parse a property value as a date
date(now())                        -- wrap now() into a date context
datetime('2024-01-15T12:30:00Z')   -- parse ISO datetime with timezone
```

### Date difference

Returns the **absolute** difference between two dates.

```
dateDiff(<date1>, <date2>, <unit>)
```

Units: `day`, `hour`, `minute`

```
dateDiff(date(now()), date(created_at), day) > 30
dateDiff(date(start), date(end), hour) < 48
```

Aliases: `dateDiff`, `datediff`, `date_diff`.

### Date addition and subtraction

```
date_add(date(now()), 7, day)
date_subtract(date(expiry), 1, day)
```

Aliases: `date_add` / `dateAdd` and `date_subtract` / `dateSubtract`.

### Day of week

```
day_of_week(date(transaction_date))
```

Returns a number: `0` = Sunday, `1` = Monday, …, `6` = Saturday.

### Examples

```
'expired'     date(expiry) < date(now())                            RETURN 'expired'
'recent'      dateDiff(date(now()), date(created_at), day) <= 7     RETURN 'new'
'future'      date_add(date(now()), 30, day) > date(deadline)       RETURN 'urgent'
'weekend'     day_of_week(date(txn_date)) IN 0, 6                   RETURN 'weekend'
```

---

## 14. String Similarity

Five functions for fuzzy string matching. All return a numeric score.

| Function | Description |
|---|---|
| `string_distance(a, b)` | Levenshtein edit distance (lower = more similar) |
| `partial_ratio(a, b)` | Fuzzy partial match score (0–100; higher = more similar) |
| `token_sort_ratio(a, b)` | Sorts tokens before comparing (good for word-order differences) |
| `token_set_ratio(a, b)` | Set-based token comparison |
| `string_similarity_score(a, b)` | General similarity score |

Each function accepts snake_case and camelCase names.

```
'name_match'  string_distance(name1, name2) < 3       RETURN 'likely_same_person'
'email_fuzzy' partial_ratio(email, known_email) > 85   RETURN 'possible_duplicate'
'doc_match'   token_sort_ratio(desc, reference) = 100  RETURN 'exact_match'
```

---

## 15. Geo Operations

### Geohash encoding / decoding

```
geohash_encode(latitude, longitude)
geohash_encode(latitude, longitude, precision)   -- precision 1–12, default 12
geohash_decode(geohash)                          -- returns [latitude, longitude]
```

### Distance

```
distance(lat1, lon1, lat2, lon2)    -- from raw coordinates (km)
distance(geohash1, geohash2)        -- from geohash strings (km)
```

### Within radius

```
within_radius(lat1, lon1, lat2, lon2, radius_km)   -- true if within radius_km
```

```
'nearby'  within_radius(user.lat, user.lon, store.lat, store.lon, 5) RETURN 'eligible'
'far'     distance(user.lat, user.lon, hq.lat, hq.lon) > 100         RETURN 'remote'
'zone'    geohash_encode(lat, lon, 6) = 'dr5reg'                     RETURN 'in_zone'
```

---

## 16. Regex

### regex_strip

Removes all characters matching the given regex pattern from a property value.

```
regex_strip(<property>, '<pattern>')
```

```
'clean_phone' regex_strip(phone_number, '[^0-9]') = '14155552671' RETURN 'valid'
'no_spaces'   regex_strip(code, '\s') = 'ABC123'                   RETURN 'valid_code'
```

Aliases: `regex_strip`, `regexStrip`, `regexstrip`.

---

## 17. Evaluation Modes

### single_match (default)

Returns on the **first** matching rule. Remaining rules and rulesets are not evaluated.

```
WORKFLOW 'my_workflow'
    -- implicit single_match
```

### multi_match

Evaluates **all** rules across all rulesets. All matching rules accumulate.

```
WORKFLOW 'my_workflow' EVALUATION_MODE MULTI_MATCH
```

The top-level `WorkflowResult` reflects the **first** match. All matches are available via `result.getMatchedRules()`:

```java
List<MatchedRuleListItem> all = result.getMatchedRules();
for (MatchedRuleListItem item : all) {
    String rule              = item.getRule();
    String ruleResult        = item.getResult();
    Map<String, Object> vars = item.getVariables();  // snapshot at match time
}
```

In `multi_match`, `set` variables accumulate across all matched rules, and each `MatchedRuleListItem` carries its own snapshot of variables at the moment it matched.

---

## 18. Error Handling and Warnings

RuleFlow is designed for resilience: individual rule failures do not abort the workflow. The failed rule is skipped with a warning and evaluation continues.

### What causes a rule to be skipped

| Situation | Warning message |
|---|---|
| Property not found in data | `"<field> field cannot be found"` |
| Type mismatch in comparison | `"There is a comparison between different dataTypes in rule <name>"` |
| Undefined custom function | `"Custom function '<name>' is not defined"` |
| Custom function throws | `"Custom function '<name>' failed: <message>"` |
| Missing property in action params | Resolution failure warning |

### Reading warnings

```java
Set<String> warnings = result.getWarnings();
boolean hadError     = result.isError();   // true only for unexpected engine-level failures
```

### Null behaviour

- `field = null` — true if the field is null or absent
- `field <> null` — true if the field has a non-null value
- Accessing a null-valued nested path (e.g. `user.address.city` when `address` is null) — rule skipped with warning
- Null field in an aggregation predicate (e.g. `it.priority > 3` where `priority` is null) — item treated as not matching, no warning

---

## 19. Case Sensitivity

| Element | Case sensitivity |
|---|---|
| Keywords (`WORKFLOW`, `RETURN`, `AND`, `IN`, …) | Case-insensitive |
| Built-in function names (`dateDiff`, `geohash_encode`, …) | Case-insensitive |
| Custom function names | **Case-sensitive** (must match registration key exactly) |
| Property names | **Case-sensitive** |
| String comparison with `=` | Case-insensitive |
| String comparison with `==` | **Case-sensitive** |
| String literals (`'US'`, `'us'`) | Treated as written |

---

## 20. Operator Precedence

From highest to lowest:

| Priority | Operators |
|---|---|
| 1 (highest) | `( )` parentheses, `abs()`, unary `-` |
| 2 | `.field` member access |
| 3 | `*`, `/`, `%`, `mod` |
| 4 | `+`, `-` |
| 5 | `<`, `<=`, `>`, `>=`, `=`, `==`, `<>` |
| 6 | `IN`, `CONTAINS`, `STARTS_WITH`, `NOT IN`, … |
| 7 | `AND` |
| 8 (lowest) | `OR` |

```
-- Parsed as: ((a + (b * c)) = 15 AND d < 10) OR e = 5
a + b * c = 15 AND d < 10 OR e = 5

-- Use parentheses to change precedence:
a + b * c = 15 AND (d < 10 OR e = 5)
```

---

## 21. Full Examples

### AML onboarding risk scoring

```
workflow 'aml_onboarding'
    ruleset 'work_info'
        'employed'    customer.workInfo.code = 1010  set $score = 0.45 * 10 continue
        'self_emp'    customer.workInfo.code = 230   set $score += 0.45 * 5 continue

    ruleset 'sanctions'
        'hit'      screening(documentNumber, customer.firstName + ' ' + customer.lastName).matchCount > 5
                   set $score = 0.55 * 15
                   action('block') continue
        'hit_list' screening(documentNumber, customer.firstName + ' ' + customer.lastName).matches.contains { it.matchPriority > 3 }
                   set $score = 0.55 * 15
                   action('block') continue
        'hit_2'    screening(documentNumber, customer.firstName + ' ' + customer.lastName).matchCount > 0
                   set $score += 0.45 * 15
                   action('block') continue

    ruleset 'risk_rating'
        'low'    $score > 3  AND $score < 15 return low
        'medium' $score >= 15 AND $score < 30 return medium
        'high'   $score >= 30 return high

    default low
end
```

### Fraud detection

```
WORKFLOW 'fraud_detection' EVALUATION_MODE SINGLE_MATCH

    RULESET 'velocity' transaction.count_24h > 50 THEN
        'burst_activity'
            transaction.count_24h > 200
            set $risk_flag = 'critical'
            RETURN 'block'
            WITH action('alert', {'channel': 'ops', 'severity': 'high'})

        'elevated_velocity'
            transaction.count_24h > 50
            set $risk_flag = 'elevated'
            RETURN 'review'

    RULESET 'device'
        'blacklisted_device'
            device.fingerprint IN list('device_blacklist')
            RETURN 'block'

        'new_suspicious_device'
            NOT (device.fingerprint IN list('known_devices'))
            AND string_distance(device.model, last_known_device) > 5
            RETURN 'step_up'

    RULESET 'location'
        'blocked_country'
            country IN list('blocked_countries')
            RETURN 'block'

        'geo_anomaly'
            within_radius(user.lat, user.lon, usual.lat, usual.lon, 500) = false
            AND dateDiff(date(now()), date(last_login), hour) < 1
            RETURN 'review'

    RULESET 'blacklists'
        'email_blacklist'
            evalInList('email_patterns', elem.pattern = user.email_domain)
            RETURN 'block'

    DEFAULT RETURN 'allow'
END
```

### Pricing engine

```
WORKFLOW 'pricing' EVALUATION_MODE SINGLE_MATCH

    RULESET 'eligibility' customer.status <> 'suspended' THEN

        'vip_bulk'
            customer.tier = 'vip' AND order.items.count() >= 10
            set $discount = 0.25
            RETURN EXPR(order.subtotal * (1 - $discount))

        'vip'
            customer.tier = 'vip'
            set $discount = 0.15
            RETURN EXPR(order.subtotal * (1 - $discount))

        'bulk'
            order.items.count() >= 10
            set $discount = 0.10
            RETURN EXPR(order.subtotal * (1 - $discount))

    DEFAULT RETURN EXPR(order.subtotal)
END
```

### Multi-match tagging

```
WORKFLOW 'tag_transaction' EVALUATION_MODE MULTI_MATCH

    RULESET 'tags'
        'high_value'    amount > 5000                              RETURN 'high_value'
        'international' country <> home_country                    RETURN 'international'
        'weekend'       day_of_week(date(created)) IN 0, 6         RETURN 'weekend'
        'new_merchant'  NOT (merchant IN list('known_merchants'))   RETURN 'new_merchant'

    DEFAULT RETURN 'standard'
END
```

```java
List<String> tags = result.getMatchedRules()
    .stream()
    .map(MatchedRuleListItem::getResult)
    .toList();
// tags = ["high_value", "international", "weekend"]
```

---

## 22. Best Practices

- **Provide a default clause** — always. It is the safety net when no rule matches.
- **Use `continue` for scoring rulesets** — rulesets that only accumulate scores or flags should use `continue`; reserve `return` for the final decision ruleset.
- **Prefer `single_match`** unless you need to collect multiple results. It is faster and simpler to reason about.
- **Use compound assignment (`+=`, `-=`, etc.)** when updating a running score in a `continue` rule rather than spelling out `$x = $x + ...`.
- **Use `it.field` for explicit current-item access** inside aggregation predicates to avoid ambiguity when field names overlap with root-level properties.
- **Use root access (`.field`)** inside collection predicates when cross-referencing root data: `items.any { price > .user.spending_limits.max }`.
- **Use named function arguments** for clarity when a custom function takes multiple parameters: `kyc(documentNumber: docId, fullName: firstName + ' ' + lastName)`.
- **Keep rule names descriptive** — they appear in `result.getRule()` and in warning messages.
- **Test missing properties** — any missing path in the request data skips the rule with a warning rather than throwing. Verify that your default covers those cases.

---

## 23. Reserved Keywords

`workflow`, `ruleset`, `default`, `end`, `return`, `with`, `and`, `or`, `not`, `then`, `continue`, `in`, `contains`, `starts_with`, `any`, `all`, `none`, `count`, `average`, `distinct`, `list`, `elem`, `it`, `action`, `set`, `true`, `false`, `null`, `evaluation_mode`, `single_match`, `multi_match`, `now`, `date`, `datetime`, `date_add`, `date_subtract`, `date_diff`, `day_of_week`, `abs`, `expr`

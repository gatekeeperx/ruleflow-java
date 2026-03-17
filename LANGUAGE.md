# RuleFlow DSL — Language Reference

RuleFlow is a declarative rule engine DSL for expressing business logic as readable, structured workflows. Rules evaluate against a data payload and produce a named result, optional actions, and optional computed variables.

---

## Table of Contents

1. [Workflow Structure](#1-workflow-structure)
   2. [Rules and Results](#2-rules-and-results)
   3. [Expressions and Conditions](#3-expressions-and-conditions)
   4. [Property Access](#4-property-access)
   5. [Literals and Values](#5-literals-and-values)
   6. [Math Operators](#6-math-operators)
   7. [List Operations](#7-list-operations)
   8. [Aggregations](#8-aggregations)
   9. [evalInList](#9-evalinlist)
   10. [Date and Time](#10-date-and-time)
   11. [String Similarity](#11-string-similarity)
   12. [Geo Operations](#12-geo-operations)
   13. [Regex](#13-regex)
   14. [Custom Functions](#14-custom-functions)
   15. [set — Variable Assignment](#15-set--variable-assignment)
   16. [continue — Score and Proceed](#16-continue--score-and-proceed)
   17. [Actions](#17-actions)
   18. [Evaluation Modes](#18-evaluation-modes)
   19. [Error Handling and Warnings](#19-error-handling-and-warnings)
   20. [Case Sensitivity](#20-case-sensitivity)
   21. [Operator Precedence](#21-operator-precedence)
   22. [Full Examples](#22-full-examples)

---

## 1. Workflow Structure

A workflow is the top-level container. It holds one or more rulesets, each containing one or more rules, and ends with a mandatory default clause.

```
WORKFLOW '<name>'
    [EVALUATION_MODE SINGLE_MATCH | MULTI_MATCH]

    RULESET '<name>' [<condition> THEN]
        '<rule_name>' <condition> [set ...] RETURN <result> [WITH <actions>]
        ...

    DEFAULT RETURN <result> [WITH <actions>]
END
```

All keywords (`WORKFLOW`, `RULESET`, `RETURN`, `THEN`, `DEFAULT`, `END`, etc.) are **case-insensitive**.

### Ruleset condition (guard)

A ruleset can be guarded by an optional condition. If the condition is false, the entire ruleset is skipped.

```
RULESET 'high_value_checks' amount > 10000 THEN
    'rule_a' ...
    'rule_b' ...
```

### Default clause

The `DEFAULT` clause is required. It fires when no rule matches or all rulesets are skipped.

```
DEFAULT RETURN 'approved'
DEFAULT allow                  -- RETURN keyword is optional
DEFAULT THEN allow             -- THEN keyword also accepted
```

---

## 2. Rules and Results

Each rule has a name (single-quoted), a condition expression, and a result.

### RETURN — named result

```
'check_amount' amount > 1000 RETURN 'block'
'check_amount' amount > 1000 RETURN block          -- quotes are optional for simple identifiers
```

### THEN — action-only result

`THEN` is used when the result is expressed purely through actions (less common):

```
'notify' score > 80 THEN action('send_alert')
```

### RETURN EXPR — dynamic result

Use `EXPR(...)` when the result should be computed from an expression rather than a fixed string:

```
'calc_price'  discount > 0 RETURN EXPR(amount * (1 - discount))
```

The expression result is converted to a string and returned as the workflow result.

---

## 3. Expressions and Conditions

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
country = 'us'          -- matches 'US', 'us', 'Us', etc.
country == 'US'         -- only matches exactly 'US'
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
'null_check'    field = null   RETURN 'missing_field'
'not_null'      field <> null  RETURN 'has_value'
```

---

## 4. Property Access

Properties are read from the request data payload passed into the workflow at evaluation time.

### Simple property

```
amount
userId
country
```

### Nested property (dot notation)

```
user.email
order.address.city
device.info.fingerprint
```

Each segment must be present in the data. If any segment is missing, the rule fails with a warning and evaluation falls through to the next rule.

### Leading dot — root context

A leading dot forces resolution from the root data context. Useful inside nested scopes (aggregation predicates, `evalInList`) to escape the local scope:

```
items.any { type = .requestedType }   -- .requestedType reads from root request data
```

### elem — list item reference

`elem` refers to the current item when iterating a list inside `evalInList`:

```
evalInList('users', elem.status = 'active')
evalInList('orders', elem.amount > 100 AND elem.country = userId.country)
```

---

## 5. Literals and Values

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
1.5e3       -- scientific notation (= 1500)
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

## 6. Math Operators

| Operator | Meaning |
|---|---|
| `+` | Addition |
| `-` | Subtraction |
| `*` | Multiplication |
| `/` | Division |
| `%` or `mod` | Modulo (remainder) |
| `abs(expr)` | Absolute value |

```
'markup'     amount * 1.2 > 500 RETURN 'expensive'
'half'       total / 2 < 100 RETURN 'low'
'even'       count % 2 = 0 RETURN 'even'
'distance'   abs(delta) > 10 RETURN 'outlier'
'combined'   (base + bonus) * multiplier > threshold RETURN 'qualified'
```

---

## 7. List Operations

### Operators

| Operator | Meaning |
|---|---|
| `IN` | Value matches any element in the list |
| `CONTAINS` | Any list element is a substring of the value |
| `STARTS_WITH` | Value starts with any element in the list |
| `NOT IN` | Value does not match any element |
| `NOT CONTAINS` | No element is a substring of the value |
| `NOT STARTS_WITH` | Value starts with none of the elements |

### Inline literal list

```
country IN 'US', 'CA', 'GB'
status NOT IN 'blocked', 'suspended'
```

### Stored list

Lists are named collections provided at runtime.

```
country IN list('allowed_countries')
device.fingerprint NOT IN list('blacklisted_devices')
```

### Single value

```
category IN 'premium'       -- single element (no trailing comma needed)
```

### Tuple lists (multi-field matching)

Match on a combination of properties simultaneously:

```
(device.fingerprint, user.ip) IN list('blocked_combos')
(country, currency) IN ('US', 'USD'), ('GB', 'GBP'), ('EU', 'EUR')
```

All fields in the tuple must match.

---

## 8. Aggregations

Aggregations operate on list-type fields in the data.

### Syntax

```
<field>.<operation> { <predicate> }
<field>.<operation> ()
```

### Operations

| Operation | Description | Return type |
|---|---|---|
| `.any { predicate }` | True if at least one item matches | Boolean |
| `.all { predicate }` | True if every item matches | Boolean |
| `.none { predicate }` | True if no item matches | Boolean |
| `.count { predicate }` | Count of matching items | Number |
| `.count()` | Total number of items | Number |
| `.average { predicate }` | Average of items matching predicate | Number |
| `.average()` | Average of all items | Number |
| `.distinct { predicate }` | Distinct values matching predicate | Collection |
| `.distinct()` | All distinct values | Collection |

### Predicate forms

Inside `{ }`, each item becomes the current data context. You can use:

- A literal value — matches items equal to that value
  - A comparison — evaluated for each item
  - A compound expression — `AND`/`OR` on item properties

```
-- Does any item equal the string 'fraud'?
tags.any { 'fraud' }

-- Does any item have type = 'restricted'?
items.any { type = 'restricted' }

-- Are all transactions above 10?
transactions.all { amount > 10 }

-- How many orders have status = 'pending'?
orders.count { status = 'pending' } >= 2

-- Average price of premium items
items.average { category = 'premium' } > 500
```

---

## 9. evalInList

`evalInList` tests each item in a **named list** (provided at runtime) against a predicate, returning `true` if any item matches.

### Syntax

```
evalInList('<list_name>', <predicate>)
```

### elem

Inside the predicate, `elem` refers to the current list item. Use `elem.field` to access fields on each item.

```
evalInList('blacklist', elem.userId = userId)
evalInList('patterns', elem.type = transaction.type AND elem.region = user.region)
evalInList('sessions', elem.deviceId = device.id AND date(elem.expiresAt) > date(now()))
```

You can reference request-level properties freely alongside `elem.*`.

### Difference from aggregations

- Aggregations (`.any`, `.all`, etc.) work on **fields within the request data**
  - `evalInList` works on **externally provided named lists**

---

## 10. Date and Time

### Getting the current date/time

```
now()             -- current ZonedDateTime
currentDate()     -- current date (alias)
```

### Parsing dates

```
date('2024-01-15')           -- parse ISO date string (yyyy-MM-dd)
date(propertyName)           -- parse a property value as a date
date(now())                  -- wrap now() into a date context
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

Aliases: `dateDiff`, `datediff`, `date_diff` — all equivalent.

### Date addition and subtraction

```
date_add(date(now()), 7, day)         -- 7 days from now
date_subtract(date(expiry), 1, day)   -- one day before expiry
```

Aliases: `date_add` / `dateAdd` and `date_subtract` / `dateSubtract`.

### Day of week

```
day_of_week(date(transaction_date))
```

Returns a number: `0` = Sunday, `1` = Monday, ..., `6` = Saturday.

```
'weekend' day_of_week(date(created_at)) IN 0, 6 RETURN 'weekend_transaction'
```

### Examples

```
'expired'       date(expiry) < date(now()) RETURN 'expired'
'recent'        dateDiff(date(now()), date(created_at), day) <= 7 RETURN 'new'
'future_date'   date_add(date(now()), 30, day) > date(deadline) RETURN 'urgent'
'weekend'       day_of_week(date(txn_date)) IN 0, 6 RETURN 'weekend'
```

---

## 11. String Similarity

Five functions for fuzzy string matching. All return a numeric score.

| Function | Description |
|---|---|
| `string_distance(a, b)` | Levenshtein edit distance (lower = more similar) |
| `partial_ratio(a, b)` | Fuzzy partial match score (0–100; higher = more similar) |
| `token_sort_ratio(a, b)` | Sorts tokens before comparing (good for word-order differences) |
| `token_set_ratio(a, b)` | Set-based token comparison |
| `string_similarity_score(a, b)` | General similarity score |

Each function accepts both snake_case and camelCase names (`string_distance` / `stringDistance`, etc.).

```
'name_match'    string_distance(name1, name2) < 3 RETURN 'likely_same_person'
'email_fuzzy'   partial_ratio(email, known_email) > 85 RETURN 'possible_duplicate'
'doc_match'     token_sort_ratio(description, reference) = 100 RETURN 'exact_match'
```

---

## 12. Geo Operations

### Geohash encoding

```
geohash_encode(latitude, longitude)
geohash_encode(latitude, longitude, precision)   -- precision 1–12, default 12
```

Returns a geohash string.

### Geohash decoding

```
geohash_decode(geohash)
```

Returns coordinates as an array `[latitude, longitude]`.

### Distance

```
distance(lat1, lon1, lat2, lon2)    -- from raw coordinates (km)
distance(geohash1, geohash2)        -- from geohash strings (km)
```

### Within radius

```
within_radius(lat1, lon1, lat2, lon2, radius_km)
```

Returns `true` if the distance between the two points is within `radius_km`.

```
'nearby'    within_radius(user.lat, user.lon, store.lat, store.lon, 5) RETURN 'eligible'
'far'       distance(user.lat, user.lon, hq.lat, hq.lon) > 100 RETURN 'remote'
'geohash'   geohash_encode(lat, lon, 6) = 'dr5reg' RETURN 'in_zone'
```

---

## 13. Regex

### regex_strip

Removes all characters matching the given regex pattern from a property value.

```
regex_strip(<property>, '<pattern>')
```

```
'clean_phone'   regex_strip(phone_number, '[^0-9]') = '14155552671' RETURN 'valid'
'no_spaces'     regex_strip(code, '\s') = 'ABC123' RETURN 'valid_code'
```

Aliases: `regex_strip`, `regexStrip`, `regexstrip`.

---

## 14. Custom Functions

Custom functions let you inject external logic — API calls, ML model scores, database lookups — into rule conditions without modifying the DSL.

### Calling a custom function

```
functionName(arg1, arg2, ...)
functionName()                   -- no-arg function
```

Arguments can be any expression: property values, literals, math, or nested function calls.

```
'screen'    screening(userId) = 'pass' RETURN 'approved'
'score'     riskScore(userId, country, amount) > 700 RETURN 'review'
'flag'      isBlocked(deviceId) RETURN 'blocked'
```

### Member access on function return value

If a function returns an object (map), you can access its fields directly:

```
screening(userId).risk_score > 500
screening(userId).details.level == 'critical'
screening(userId).tags.any { 'fraud' }
```

### Registering functions (Java API)

```java
RuleflowFunction screeningFn = args -> {
    String userId = (String) args.get(0);
    // call your service
    return Map.of("risk_score", 750, "label", "high", "tags", List.of("fraud"));
};

workflow.evaluate(requestData, lists, Map.of("screening", screeningFn));
```

### Memoization

Each unique combination of `(functionName, args)` is computed **at most once** per workflow evaluation. If `screening(userId)` appears in multiple rule conditions within the same workflow run, the function is only invoked once. Subsequent references use the cached result.

---

## 15. set — Variable Assignment

`set` clauses compute and store intermediate values when a rule fires. Variables are available to all subsequent rules and rulesets within the same workflow evaluation.

### Syntax

Variable names are prefixed with `$` in both declarations and references:

```
'<rule_name>'  <condition>
    set $variable = <expression>
    set $variable2 = <expression2>
    RETURN <result>
```

- Multiple `set` clauses execute top-to-bottom
  - Later `set` clauses can reference variables set earlier in the same rule
  - Variables are workflow-scoped: visible in all subsequent rules and rulesets

### Accessing set variables

Use the `$` prefix to reference a variable anywhere an expression is valid:

```
workflow 'pricing'
    ruleset 'scoring'
        'compute' amount > 0
            set $base_score = amount * riskMultiplier
            RETURN 'scored'

    ruleset 'decision'
        'block_high' $base_score > 5000 RETURN 'block'   -- references variable from previous ruleset

    DEFAULT RETURN 'allow'
END
```

**Namespacing:** bare identifiers (e.g. `amount`) always read request data; `$`-prefixed identifiers (e.g. `$amount`) always read set variables. The two namespaces are syntactically separate — there is no ambiguity or shadowing.

### Member access on variable values

If a set variable holds an object (map), you can access its fields with dot notation:

```
set $result = screening(userId)
$result.risk_score > 500
```

### Reading variables from the result

Keys in `getVariables()` are bare names (without `$`):

```java
WorkflowResult result = workflow.evaluate(data, lists);
Map<String, Object> vars = result.getVariables();
Object score = vars.get("base_score");
```

### Examples

```
'high_risk'
    score > 800
    set $risk_label = 'critical'
    set $fee = amount * 0.05
    RETURN 'flagged'
    WITH action('notify', {'level': 'critical'})

'medium_risk'
    score > 500
    set $risk_label = 'medium'
    RETURN 'review'
```

---

## 16. continue — Score and Proceed

`continue` lets a rule fire its `set` clauses and then keep evaluating — without returning a final result. This is the standard pattern for multi-step scoring workflows where some rulesets assign scores and later rulesets make decisions based on those scores.

### Syntax

```
'<rule_name>'  <condition>
    [set $variable = <expression> ...]
    continue
```

A rule with `continue`:
- Evaluates its condition normally
- Executes all `set` clauses if the condition is true
- Does **not** return a result — evaluation continues to the next rule in the same ruleset, and then to subsequent rulesets
- Does **not** appear in `getMatchedRules()` (even in `multi_match` mode)

### Example: multi-step onboarding risk

```
workflow 'onboarding'
    ruleset 'score_occupation'
        'farmer'  occupation == 'farmer'  set $occ = 10 continue
        'student' occupation == 'student' set $occ = 3  continue

    ruleset 'score_country'
        'high_risk' country == 'XX'                          set $nat = 10 continue
        'local'     country == 'CO'                          set $nat = 5  continue
        'other'     country <> 'XX' AND country <> 'CO'      set $nat = 7  continue

    ruleset 'risk_rating'
        'high'   ($occ * 0.5) + ($nat * 0.5) > 7 return high_risk
        'medium' ($occ * 0.5) + ($nat * 0.5) > 4 return medium_risk

    default low_risk
end
```

The first two rulesets set `$occ` and `$nat`; the third ruleset reads them to produce a final decision.

### continue vs return with set

Use `continue` when you want scoring rulesets that feed into a decision ruleset. Use `return` with `set` when the rule that sets the variable also returns the final result.

### Falling to default

If no subsequent rule matches after all `continue` rules have fired, the `default` clause is returned. Variables set by `continue` rules are available in `getVariables()` even when the default is returned.

---

## 17. Actions

Actions describe side effects to execute when a rule fires. They are returned as part of the workflow result for the caller to process.

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

Parameter values can be:
- String literals: `'email'`, `'high'`
  - Request properties: `userId`, `order.id`

```
action('review', { 'user': userId, 'amount': amount, 'reason': 'velocity' })
```

### Reading actions from the result

```java
List<Action> actions = result.getActionCalls();
for (Action a : actions) {
    String name = a.getName();
    Map<String, String> params = a.getParams();
}
```

---

## 18. Evaluation Modes

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

The top-level `WorkflowResult` reflects the **first** match (for backward compatibility). All matches are available via `result.getMatchedRules()`.

```java
List<MatchedRuleListItem> all = result.getMatchedRules();
```

In multi_match mode, `set` variables accumulate across all matched rules, and the final `getVariables()` snapshot reflects all assignments made throughout the evaluation.

---

## 19. Error Handling and Warnings

RuleFlow is designed for resilience: individual rule failures do not abort the workflow. Instead, the failed rule is skipped with a warning, and evaluation continues.

### What causes a rule to be skipped

| Situation | Warning message |
|---|---|
| Property not found in data | `"<field> field cannot be found"` |
| Type mismatch in comparison | `"There is a comparison between different dataTypes in rule <name>"` |
| Undefined custom function | `"Custom function '<name>' is not defined"` |
| Missing property in action params | Resolution failure warning |

### Reading warnings

```java
Set<String> warnings = result.getWarnings();
boolean hadError = result.isError();
```

### Null behavior

- `field = null` — true if field is null or absent
  - `field <> null` — true if field has a value
  - Accessing a null-valued nested field (e.g., `user.address.city` when `address` is null) — rule skipped with warning

---

## 20. Case Sensitivity

| Element | Case sensitivity |
|---|---|
| Keywords (`WORKFLOW`, `RETURN`, `AND`, `IN`, …) | Case-insensitive |
| Built-in function names (`dateDiff`, `geohash_encode`, …) | Case-insensitive |
| Custom function names | Case-sensitive (must match registration key) |
| Property names | Case-sensitive |
| String comparison with `=` | Case-insensitive |
| String comparison with `==` | Case-sensitive |
| String literals (`'US'`, `'us'`) | Treated as written |

---

## 21. Operator Precedence

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
-- Parsed as: ((a + (b * c)) = 15) AND (d < 10)) OR (e = 5)
a + b * c = 15 AND d < 10 OR e = 5

-- Use parentheses to change precedence:
a + b * c = 15 AND (d < 10 OR e = 5)
```

---

## 22. Full Examples

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
            set $label = 'vip_bulk'
            RETURN EXPR(order.subtotal * (1 - $discount))

        'vip'
            customer.tier = 'vip'
            set $discount = 0.15
            set $label = 'vip'
            RETURN EXPR(order.subtotal * (1 - $discount))

        'bulk'
            order.items.count() >= 10
            set $discount = 0.10
            set $label = 'bulk'
            RETURN EXPR(order.subtotal * (1 - $discount))

    DEFAULT RETURN EXPR(order.subtotal)
END
```

### Multi-match tagging

```
WORKFLOW 'tag_transaction' EVALUATION_MODE MULTI_MATCH

    RULESET 'tags'
        'high_value'    amount > 5000              RETURN 'high_value'
        'international' country <> home_country    RETURN 'international'
        'weekend'       day_of_week(date(created)) IN 0, 6  RETURN 'weekend'
        'new_merchant'  NOT (merchant IN list('known_merchants')) RETURN 'new_merchant'

    DEFAULT RETURN 'standard'
END
```

```java
WorkflowResult result = workflow.evaluate(data, lists);
List<String> tags = result.getMatchedRules()
    .stream()
    .map(MatchedRuleListItem::getResult)
    .toList();
// tags = ["high_value", "international", "weekend"]
```
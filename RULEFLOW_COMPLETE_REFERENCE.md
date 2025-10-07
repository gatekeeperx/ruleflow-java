# RuleFlow Complete Language Reference

## Table of Contents
1. [Introduction](#introduction)
2. [Quick Start](#quick-start)
3. [Workflow Structure](#workflow-structure)
4. [Configuration Options](#configuration-options)
5. [Operators Reference](#operators-reference)
6. [Rule Syntax](#rule-syntax)
7. [Data Types and Literals](#data-types-and-literals)
8. [Functions by Category](#functions-by-category)
9. [Property Access](#property-access)
10. [Actions](#actions)
11. [Error Handling](#error-handling)
12. [Complete Examples](#complete-examples)
13. [Best Practices](#best-practices)
14. [Language Reference](#language-reference)

---

## Introduction

The RuleFlow Language is a domain-specific language (DSL) designed to define and evaluate business rules and workflows in a readable, maintainable format. This complete reference documents all language syntax and functionality as implemented in the RuleFlow system.

**Key Features:**
- Workflow-based rule organization
- Rich expression language with mathematical, logical, and comparison operations
- Advanced string similarity and geospatial functions
- Flexible action system with parameters
- Comprehensive error handling and warnings

---

## Quick Start

### Simple Example
```text
workflow 'fraud_detection'
    ruleset 'basic_checks'
        'high_amount' amount > 1000 return review with manual_check
        'blocked_country' country in 'XX', 'YY' return block
    default return allow
end
```

### Java Usage
```java
import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;

Workflow wf = new Workflow(workflowString);
WorkflowResult result = wf.evaluate(Map.of("amount", 1500, "country", "US"));
```

---

## Workflow Structure

### Basic Structure
```text
workflow 'workflow_name'
    [configuration]
    ruleset 'ruleset_name' [condition] [then]
        'rule_name' expression return result [with actions]
        'rule_name2' expression return result
    ruleset 'another_ruleset'
        'rule_name3' expression return result
    default [return] result [with actions]
end
```

### Complete Example
```text
workflow 'fraud_detection'
    evaluation_mode multi_match
    ruleset 'premium_user_checks' user.is_premium = true then
        'premium_velocity' features.approved_qty_90d >= features.approved_qty_60d + 6
            and features.approved_amount_90d >= features.approved_amount_30d + 100
            return allow
        'premium_anomaly' features.is_card_bin_in_anomaly_detector = true 
            return block with manual_review({'type': 'premium_user_anomaly'})
    ruleset 'geo_checks'
        'location_velocity' distance(user.current_lat, user.current_lon, user.last_lat, user.last_lon) > 1000
            and date_diff(user.last_login, now(), hour) < 2
            return block with action('geo_impossible')
    default return allow with log_decision
end
```

---

## Configuration Options

### Evaluation Modes
Control how rules are evaluated within a workflow:
- `evaluation_mode single_match` - Stop after first matching rule (default)
- `evaluation_mode multi_match` - Continue evaluating all rules

```text
workflow 'test'
    evaluation_mode multi_match
    ruleset 'checks'
        'rule1' condition1 return result1
        'rule2' condition2 return result2
    default allow
end
```

---

## Operators Reference

### 1. Boolean Operators
Boolean operators combine or negate conditions in rule expressions.

| Operator | Description           | Example |
|----------|----------------------|---------|
| AND      | Logical AND          | `x AND y` |
| OR       | Logical OR           | `x OR y`  |
| NOT      | Logical negation     | `NOT x` or `x NOT IN list('a', 'b')` |

**Examples:**
```text
'rule1' age > 18 AND country = 'US' return allow
'rule2' NOT is_blacklisted return allow
'precedence' (x OR y) AND z return block
```

**Operator Precedence:** `NOT` → `AND` → `OR`

### 2. Comparison Operators
Used to compare values, numbers, strings, or fields.

| Operator | Description         | Example |
|----------|--------------------|---------|
| =, ==    | Equal to           | `x = 10`, `status == 'OK'` |
| <>       | Not equal to       | `country <> 'US'` |
| <        | Less than          | `score < 100` |
| <=       | Less than or equal | `score <= 99` |
| >        | Greater than       | `score > 0` |
| >=       | Greater or equal   | `score >= 1` |

**Examples:**
```text
'rule3' amount >= 1000 return block
'rule4' status <> 'APPROVED' return review
'string_compare' name < 'Smith' return alphabetical_order
```

### 3. Mathematical Operators
Perform arithmetic calculations in expressions.

| Operator | Description     | Example |
|----------|----------------|---------|
| +        | Addition       | `x + y` |
| -        | Subtraction    | `x - y` |
| *        | Multiplication | `x * y` |
| /        | Division       | `x / y` |
| % / mod  | Modulo         | `x % y`, `x mod y` |

**Examples:**
```text
'rule5' (price * quantity) > 1000 return block
'rule6' order_id % 10 = 0 return sample
'precedence' x + y * z = 15 return block  # multiplication first
'parentheses' (x + y) * z = 80 return block
```

**Operator Precedence:** `*`, `/`, `%` → `+`, `-`

### 4. List and Collection Operators
Used to work with lists, sets, and collections.

| Operator      | Description                       | Example |
|---------------|-----------------------------------|---------|
| IN            | Value is in list/tuple            | `country IN 'US', 'CA', 'MX'` |
| NOT IN        | Value is not in list/tuple        | `status NOT IN 'OK', 'PENDING'` |
| CONTAINS      | List contains value               | `tags CONTAINS 'urgent'` |
| STARTS_WITH   | String starts with prefix         | `email STARTS_WITH 'admin@'` |
| ANY           | Any element matches predicate     | `items.ANY { type = 'restricted' }` |
| ALL           | All elements match predicate      | `items.ALL { verified = true }` |
| NONE          | No element matches predicate      | `items.NONE { flagged = true }` |

**Examples:**
```text
'rule7' user.role IN 'admin', 'manager' return allow
'rule8' order.items.ANY { price > 100 } return review
'rule9' email STARTS_WITH 'test@' return block
'stored_list' user_id in list('blocked_users') return block
'tuple_check' (user.country, user.state) in ('US', 'CA'), ('US', 'NY') return domestic
```

#### Direct Value Comparisons in Aggregation Predicates
The aggregation operators (`.any`, `.all`, `.none`) support two types of predicates:

**1. Property Comparisons** - Compare properties of list items:
```text
'property_match' order.items.any { type = 'restricted' } return block
'bulk_items' order.items.any { quantity > 100 } return review
'verified_items' order.items.all { verified = true } return trusted
```

**2. Direct Value Comparisons** - Compare list items directly to values:
```text
'string_match' customer.tags.any {'blocked'} return block
'numeric_match' user.scores.any {100} return high_score
'boolean_match' user.flags.any {true} return has_flag
'not_found' customer.tags.none {'premium'} return standard_user
```

**Data Structure Examples:**
```json
{
  "customer": {
    "tags": ["blocked", "risky", "verified"]
  },
  "user": {
    "scores": [85, 100, 92],
    "flags": [false, true, false]
  }
}
```

### 5. Aggregation Operators
Perform calculations on collections.

| Function  | Description                       | Example |
|-----------|-----------------------------------|---------|
| COUNT     | Number of elements               | `items.COUNT() > 5` |
| AVERAGE   | Average value of elements        | `items.AVERAGE { price } > 100` |
| DISTINCT  | Unique elements in collection    | `items.DISTINCT { vendor_id }.COUNT() > 3` |

**Examples:**
```text
'count_check' order.items.count() > 5 return review
'average_price' order.items.average { price } > 100 return premium
'multi_vendor' order.items.distinct { vendor_id }.count() > 3 return complex_order
```

---

## Rule Syntax

Rules support two main syntaxes:

### 1. Return Syntax (Most Common)
```text
'rule_name' expression return result [with actions]
```

### 2. Then Syntax (Action-First)
```text
'rule_name' (expression then action(...))
```

### 3. Ruleset Conditions
Rulesets can have optional conditions:
```text
ruleset 'premium_user_rules' user.is_premium = true then
    'rule1' amount > 1000 return block
    'rule2' velocity > 5 return review

ruleset 'standard_rules' user.is_premium <> true then
    'rule1' amount > 100 return review

ruleset 'always_evaluated'
    'rule1' suspicious_activity = true return block
```

### 4. Parentheses
Parentheses around rule bodies are optional:
```text
'rule1' (expression return result)
'rule2' expression return result
```

---

## Data Types and Literals

### String Literals
```text
'single_quotes' name = 'John Doe' return match
'special_chars' code = 'ABC-123_XYZ' return valid
```

### Numeric Literals
```text
'integer' count = 42 return valid
'decimal' rate = 3.14159 return calculated
'negative' balance = -100.50 return overdraft
'scientific' threshold = 1.5e10 return high
```

### Boolean Literals
```text
'boolean_true' active = true return enabled
'boolean_false' deleted = false return available
```

### Null Values
```text
'null_check' middle_name = null return no_middle_name
```

---

## Functions by Category

### Mathematical Functions

| Function | Description              | Example |
|----------|--------------------------|---------|
| abs      | Absolute value           | `abs(balance) > 1000` |

**Examples:**
```text
'absolute_value' abs(balance) > 1000 return high_magnitude
'absolute_diff' abs(actual - expected) < 0.01 return within_tolerance
```

### Date and DateTime Functions

| Function          | Description                              | Example |
|-------------------|------------------------------------------|---------|
| date              | Cast to date                            | `date('2024-06-01')` |
| datetime          | Cast to datetime                        | `datetime('2024-06-01T12:30Z')` |
| now               | Current date/time                       | `now()` |
| date_add          | Add time to date                        | `date_add(date, 5, day)` |
| date_subtract     | Subtract time from date                 | `date_subtract(date, 2, hour)` |
| date_diff         | Difference between dates                | `date_diff(start, end, day)` |
| day_of_week       | Day of week for date                    | `day_of_week(date)` |

**Supported Time Units:** `day`, `hour`, `minute`
**Day Values:** `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

**Examples:**
```text
'date_literal' order_date = date('2024-06-01') return valid
'datetime_literal' timestamp = datetime('2024-06-01T12:30Z') return valid
'now_check' now() > date('2024-01-01') return current_year
'add_days' date_add(order_date, 5, day) = date('2024-06-06') return future
'date_diff' date_diff(start_date, end_date, day) = 30 return month_duration
'weekend' day_of_week(order_date) in 'SATURDAY', 'SUNDAY' return weekend_order
```

### String Functions

| Function                | Description                           | Example |
|-------------------------|---------------------------------------|---------|
| regex_strip             | Remove regex matches from string      | `regex_strip(phone, '^\\+1')` |
| string_distance         | Levenshtein-based similarity (0-100)  | `string_distance(name1, name2)` |
| partial_ratio           | Best substring match (0-100)         | `partial_ratio(addr1, addr2)` |
| token_sort_ratio        | Ignore word order (0-100)            | `token_sort_ratio(name1, name2)` |
| token_set_ratio         | Set-based comparison (0-100)         | `token_set_ratio(desc1, desc2)` |
| string_similarity_score | Best overall score (0-100)           | `string_similarity_score(text1, text2)` |

**Examples:**
```text
'strip_prefix' regex_strip(phone, '^\\+1') = '5551234567' return us_number
'similar_names' string_distance(input_name, 'John Smith') > 70 return likely_match
'partial_match' partial_ratio(address1, address2) > 80 return similar_address
'similarity_score' string_similarity_score(text1, text2) > 90 return high_similarity
```

### Geospatial Functions

| Function        | Description                               | Example |
|-----------------|-------------------------------------------|---------|
| geohash_encode  | Encode lat/lon to geohash                | `geohash_encode(lat, lon, 8)` |
| geohash_decode  | Decode geohash to coordinates            | `geohash_decode('9q8yyk8y')` |
| distance        | Distance between points (km)             | `distance(lat1, lon1, lat2, lon2)` |
| within_radius   | Check if points within radius            | `within_radius(lat1, lon1, lat2, lon2, 50)` |

**Examples:**
```text
'encode_location' geohash_encode(lat, lon, 8) = '9q8yyk8y' return sf_area
'coord_distance' distance(lat1, lon1, lat2, lon2) < 100 return nearby
'within_radius' within_radius(user_lat, user_lon, store_lat, store_lon, 50) return local_delivery
```

---

## Property Access

RuleFlow uses dot notation to access properties in nested data structures. The system supports sophisticated scoping rules that allow flexible property access in different contexts.

### Basic Property Access

#### Simple Properties
```text
'simple' user_id = 123 return match
'basic_field' amount > 1000 return high_value
```

#### Nested Properties
```text
'nested' user.profile.email = 'admin@company.com' return admin
'deep_nested' order.payment.card.type = 'visa' return visa_card
'chain_access' request.shipping.address.country = 'US' return domestic
```

### Root vs Context-Sensitive Access

#### Leading Dot (.) - Root Access
When a property starts with a dot (`.`), it **always** accesses from the root context:

```text
'root_access' .global_config = 'enabled' return configured
'force_root' .user.settings.theme = 'dark' return dark_mode
```

#### No Leading Dot - Context-Sensitive Access
Without a leading dot, property access follows scoping rules:

```text
'context_sensitive' user_id = 15 return match
'nested_context' order.items.any { price > current_limit } return expensive
```

### Scoping Rules in Collections

When working with collections (using `.any`, `.all`, `.none`), RuleFlow creates nested scopes:

#### Current Item Scope
Inside collection predicates, properties without leading dots first look in the current item:

```text
workflow 'scope_demo'
    ruleset 'collection_access'
        # 'type' accesses the type field of each item in order.items
        'restricted_items' order.items.any { type = 'restricted' } return block
        
        # 'quantity' accesses quantity field of each item
        'bulk_items' order.items.any { quantity > 100 } return bulk_order
    default allow
end
```

**Data Structure:**
```json
{
  "order": {
    "items": [
      {"type": "normal", "quantity": 5},
      {"type": "restricted", "quantity": 2}
    ]
  }
}
```

#### Parent Context Access
Properties not found in the current item scope fall back to parent contexts:

```text
workflow 'parent_scope'
    ruleset 'context_fallback'
        # 'user_limit' not in item, so accesses from parent context
        'over_limit' order.items.any { quantity > user_limit } return excessive
    default allow
end
```

#### Root Context Override
Use leading dot to force access from root context, bypassing current item scope:

```text
workflow 'root_override'
    ruleset 'explicit_root'
        # Compare item's order_id with root-level order_id
        'id_mismatch' order.items.any { order_id <> .order_id } return inconsistent
        
        # Access global configuration from within item context
        'config_check' order.items.any { price > .global_limits.max_item_price } return over_limit
    default allow
end
```

### Advanced Examples

#### Complex Nested Access
```text
workflow 'complex_access'
    ruleset 'nested_examples'
        # Multi-level nesting with collections
        'multi_vendor' order.items.any { 
            vendor.products.any { category = 'restricted' } 
        } return blocked_vendor
        
        # Cross-referencing between different parts of data
        'shipping_mismatch' order.items.any { 
            shipping_class <> .order.shipping.default_class 
        } return shipping_conflict
        
        # Combining aggregations with property access
        'expensive_order' order.line_items.average { 
            price * quantity 
        } > user.spending_limits.average_item return review
    default allow
end
```

#### Scope Resolution Examples
```text
workflow 'scope_resolution'
    ruleset 'scoping_demo'
        # Without leading dot: checks item first, then parent contexts
        'context_search' request.orders.any { 
            compare_order_id = order_id  # Looks for order_id in current order item first
        } return match
        
        # With leading dot: always from root
        'root_forced' request.orders.any { 
            compare_order_id = .request.order_id  # Always from root request.order_id
        } return root_match
    default allow
end
```

### Collection Operations with Properties

#### Aggregation Functions
```text
'item_count' order.items.count() > 10 return many_items
'average_price' order.items.average { price } > 100 return expensive
'total_weight' order.items.sum { weight } > 50 return heavy_package
'unique_vendors' order.items.distinct { vendor_id }.count() > 3 return multi_vendor
```

#### Quantifier Operations
```text
'any_restricted' order.items.any { type = 'restricted' } return blocked
'all_verified' order.items.all { verified = true } return trusted
'none_flagged' user.devices.none { flagged = true } return clean_devices
```

### Property Path Resolution

#### Simple Path
```text
user_id          # Direct property access
```

#### Nested Path
```text
user.profile.email     # Navigate: user → profile → email
order.shipping.address.country  # Navigate: order → shipping → address → country
```

#### Root-Anchored Path
```text
.global_config         # Start from root, access global_config
.user.preferences.theme  # Start from root, navigate: user → preferences → theme
```

### Error Handling

#### Missing Properties
If a property doesn't exist, RuleFlow generates a warning and uses the default result:

```text
'missing_prop' non_existent_field = 'value' return match
# Generates warning: "non_existent_field field cannot be found"
# Returns default result
```

#### Nested Missing Properties
```text
'missing_nested' user.profile.missing_field = 'value' return match
# If user.profile exists but missing_field doesn't:
# Generates warning: "missing_field field cannot be found"

'missing_parent' missing_user.profile.email = 'admin' return match
# If missing_user doesn't exist:
# Generates warning: "missing_user field cannot be found"
```

### Best Practices

#### 1. Use Descriptive Property Names
```text
# Good
'high_value_user' user.profile.tier = 'premium' return priority
'international_shipping' order.shipping.country <> user.billing.country return verify

# Avoid
'check1' a.b.c = 'x' return y
```

#### 2. Prefer Context-Sensitive Access
```text
# Good - leverages natural scoping
'bulk_items' order.items.any { quantity > 100 } return bulk

# Less natural - explicit root access when not needed
'bulk_items' order.items.any { quantity > .bulk_threshold } return bulk
```

#### 3. Use Root Access for Cross-References
```text
# Good - explicit root access for clarity
'price_comparison' order.items.any { price > .user.spending_limits.max_item } return over_limit

# Ambiguous - could refer to item.user or root.user
'price_comparison' order.items.any { price > user.spending_limits.max_item } return over_limit
```

#### 4. Handle Missing Properties Gracefully
Always provide sensible defaults and consider that properties might not exist in all data scenarios.

---

## Actions

Actions are triggered when rules match and can be chained together.

### Action Syntax Variations

#### Explicit Action Function
```text
'rule1' condition return result with action('manual_review')
'rule2' condition return result with action('escalate', {'priority': 'high', 'reason': 'fraud'})
```

#### Shorthand Action Syntax
```text
'rule3' condition return result with manual_review
'rule4' condition return result with escalate({'priority': 'high'})
```

#### Multiple Actions
```text
'multi_action' condition return block with manual_review({'priority': 'high'}) and logout_user
'chain_actions' condition return block with action('review') and action('notify', {'email': 'admin@company.com'})
```

### Action Parameters
```text
'with_params' suspicious = true return block with manual_review({
    'priority': 'high',
    'reason': 'suspicious_activity',
    'reviewer': 'fraud_team'
}) and send_alert({'channel': 'slack'})
```

### Actions in Default Clause
```text
default return allow with action('log_decision', {'rule': 'default'})
```

---

## Error Handling and Warnings

The RuleFlow engine provides robust error handling:

### Missing Fields
If a referenced field doesn't exist in the input data:
- A warning is generated: `"field_name field cannot be found"`
- The rule evaluation stops and the default result is returned

### Division by Zero
```text
'safe_division' amount / quantity > 100 return expensive
# If quantity = 0, generates warning and returns default
```

### Invalid Date Formats
```text
'date_validation' date(invalid_date) = date('2024-01-01') return match
# Invalid date strings generate warnings and return default
```

### Type Mismatches
The engine handles type coercion gracefully but may generate warnings for unexpected type operations.

---

## Complete Examples

### Fraud Detection Workflow
```text
workflow 'fraud_detection'
    evaluation_mode multi_match
    
    ruleset 'premium_user_checks' order.custom.user_is_premium = true then
        'premium_velocity' features.mk_payer_approved_qty_90d >= features.mk_payer_approved_qty_60d + 6
            and features.mk_payer_approved_amount_90d >= features.mk_payer_approved_amount_30d + 100
            and features.user_compensations_amount_90d / features.mk_payer_approved_amount_90d < 0.05
            return allow
            
        'premium_anomaly' features.is_card_bin_in_anomaly_detector = true 
            or features.fake_users_user_email_score > 1 
            return block with manual_review({'type': 'premium_user_anomaly'})
    
    ruleset 'geo_checks'
        'location_velocity' distance(user.current_lat, user.current_lon, user.last_lat, user.last_lon) > 1000
            and date_diff(user.last_login, now(), hour) < 2
            return block with action('geo_impossible', {'distance': 'distance_value'})
            
        'restricted_location' within_radius(user.lat, user.lon, restricted_zone.lat, restricted_zone.lon, 10)
            return block with escalate({'reason': 'restricted_area'})
    
    ruleset 'pattern_matching'
        'similar_merchant' string_distance(merchant.name, known_fraudster.name) > 85
            return review with flag_similarity({'match_score': 'similarity_value'})
            
        'suspicious_email' regex_strip(user.email, '@.*') in list('known_fraud_patterns')
            return block with immediate_review
    
    default return allow with log_decision({'workflow': 'fraud_detection'})
end
```

### E-commerce Risk Assessment
```text
workflow 'ecommerce_risk'
    ruleset 'order_validation'
        'high_value' order.total > 5000 and user.account_age_days < 30
            return manual_review with escalate({'amount': order.total})
            
        'bulk_order' order.items.count() > 20 
            or order.items.any { quantity > 100 }
            return review with bulk_order_check
            
        'international' user.country <> order.shipping.country
            and distance(user.country_lat, user.country_lon, order.shipping.lat, order.shipping.lon) > 2000
            return verify_shipping with international_alert
    
    ruleset 'user_behavior'
        'velocity_check' user.orders_today > 5 
            and user.orders.all { status = 'completed' }
            return velocity_limit with rate_limit_user
            
        'device_fingerprint' user.devices.distinct { fingerprint }.count() > 10
            and date_diff(user.first_login, now(), day) < 7
            return suspicious_device with device_review
    
    default return approve
end
```

### Customer Tag and Score Analysis
```text
workflow 'customer_analysis'
    ruleset 'tag_based_rules'
        # Direct value comparisons with string lists
        'blocked_customer' customer.tags.any {'blocked'} return block
        'premium_customer' customer.tags.any {'premium'} return allow
        'risky_customer' customer.tags.any {'risky', 'suspicious'} return review
        
        # Direct value comparisons with numeric lists
        'high_score' user.scores.any {100} return excellent
        'low_score' user.scores.all {score < 50} return poor_performance
        
        # Direct value comparisons with boolean lists
        'has_flags' user.flags.any {true} return flagged
        'no_flags' user.flags.none {true} return clean
        
        # Mixed property and direct value comparisons
        'verified_items' order.items.all { verified = true } return trusted
        'restricted_tags' customer.tags.any {'restricted'} return blocked
    
    ruleset 'score_analysis'
        'average_score' user.scores.average {score} > 80 return high_performer
        'score_count' user.scores.count() > 10 return frequent_user
        
    default return standard
end
```

**Data Structure for Customer Analysis:**
```json
{
  "customer": {
    "tags": ["premium", "verified", "risky"]
  },
  "user": {
    "scores": [85, 100, 92, 78],
    "flags": [false, true, false, false]
  },
  "order": {
    "items": [
      {"verified": true, "price": 50},
      {"verified": false, "price": 25}
    ]
  }
}
```

---

## Best Practices

### Expression Clarity
- Use parentheses to clarify precedence in complex expressions
- Break long conditions across multiple lines for readability
- Use meaningful rule names that describe the business logic

### Error Handling
- Always provide a default clause
- Consider field existence when referencing nested properties
- Test edge cases like division by zero and invalid dates

### Performance Considerations
- Place most selective conditions first in AND expressions
- Use `single_match` evaluation mode when appropriate
- Consider using stored lists for frequently referenced data

### Action Design
- Use descriptive action names and parameter keys
- Include relevant context in action parameters
- Chain related actions together logically

---

## Language Reference

### Reserved Keywords
`workflow`, `ruleset`, `default`, `end`, `return`, `with`, `and`, `or`, `not`, `then`, `in`, `contains`, `starts_with`, `any`, `all`, `none`, `count`, `average`, `distinct`, `list`, `action`, `true`, `false`, `null`, `evaluation_mode`, `single_match`, `multi_match`

### Case Sensitivity
- Keywords are case-insensitive: `WORKFLOW`, `workflow`, `Workflow` all work
- Property names and string literals are case-sensitive
- Function names are case-insensitive with multiple variations supported

### Comments and Formatting
- Comments: Single-line `--` and multi-line `/* */`
- String literals: Single quotes `'text'`
- Identifiers: Letters, numbers, underscores; must start with letter or underscore
- Whitespace: Spaces, tabs, newlines are ignored outside of string literals

### Field and Property Access
- **Dot notation** for nested fields: `user.profile.email`, `order.shipping.address.country`
- **Leading dot (.)** forces root context access: `.global_config`, `.user.settings.theme`
- **Context-sensitive access** without leading dot: `user_id`, `amount` (searches current scope first)
- **Collection scoping** in predicates: `order.items.any { type = 'restricted' }` (type refers to item.type)
- **Root override in collections**: `items.any { price > .global_limits.max }` (forces root access)
- **Aggregation operations**: `items.count()`, `items.average { price }`, `items.distinct { vendor_id }`
- **Quantifier operations**: `items.any { condition }`, `items.all { condition }`, `items.none { condition }`

---

*This documentation reflects the complete implemented functionality as of the current version. For additional examples and edge cases, refer to the test suite in the project repository.* 
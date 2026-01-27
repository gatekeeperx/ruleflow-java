![tag jdk17](https://img.shields.io/badge/tag-jdk17-orange.svg)
![technology Maven](https://img.shields.io/badge/technology-Maven-blue.svg)
![technology ANTLR4](https://img.shields.io/badge/technology-ANTLR4-red.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.gatekeeperx/ruleflow.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.gatekeeperx/ruleflow/)

# Ruleflow Engine and DSL
### Project move and license change
- The repository has moved from `github.com/iamrenny/ruleflow` to `github.com/gatekeeperx/ruleflow`. Huge thanks to **gatekeeperx** for supporting and stewarding the project.
- Next steps for users:
  - Update any bookmarks and Git remotes to point to `https://github.com/gatekeeperx/ruleflow-java`.
  - Open new issues and pull requests in the new repository.
  - Existing Maven coordinates remain the same for now. If coordinates change in the future, we will announce it in releases and this README.
  - Consider watching and starring the new repository to keep receiving updates.

The project license has changed from Apache 2.0 to MIT. See the updated `LICENSE` file for details.

This project provides a rule-based workflow engine with a custom Domain-Specific Language (DSL) for risk evaluation. The DSL supports defining workflows, rulesets, and complex conditions with various operators to evaluate risk and determine actions. Key features include support for mathematical, logical, list, date, and aggregate operators, along with customizable return states, tags, and actions to control behavior based on evaluation outcomes.

## Latest Release


## Prerequisites
Java
Maven or Gradle

## Installation

### Maven
To use Ruleflow in your Maven project, add the following dependency to your pom.xml:
```xml
<dependency>
  <groupId>com.gatekeeperx</groupId>
  <artifactId>ruleflow</artifactId>
  <version>latest</version>
</dependency>
```

### Gradle
For Gradle users, add the following to your build.gradle:

```
implementation 'com.gatekeeperx.ruleflow:rules-interpreter:0.0.1'
```

## Usage

### Quick Start

Here's how you can get started with **Ruleflow** in Java:

#### 1. **Define a Workflow**
Create a workflow with rules using the `Workflow` class.

```java
package com.example;

import com.gatekeeperx.ruleflow.Workflow;
import vo.com.gatekeeperx.ruleflow.WorkflowResult;

import java.util.Map;

public class MainClass {
    public static void main(String[] args) {
        Workflow wf = new Workflow("""
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review')
                default allow
            end
        """);

        WorkflowResult result = wf.evaluate(Map.of("user_id", 15));

        System.out.println(result);
    }
}
```

#### 2. **Run and Evaluate the Workflow**
The above code initializes a workflow with a simple rule that blocks a user with an ID of 15 and triggers a `manual_review` action. When evaluated with the input `user_id = 15`, the result will be:

```
WorkflowResult{workflow='test', ruleSet='dummy', rule='rule_a', result='block', actions=[manual_review], warnings=[], actionsWithParams={manual_review={}}, workflowInfo=null, error=false}
```

---



## RuleFlowLanguage Language Reference

### Basic Workflow Syntax
A workflow is a collection of rules that return a risk
```workflow <workflow name>
    <rule_name> <condition> return <risk> [with <action> [AND <action> ...]]
    ...
    default <risk>
end
```

#### Example
```
 workflow 'my_rule'
    ruleset 'my_ruleset'
        'high risk user' user_id = 15 return block with action('manual_review', {'test': 'me', 'foo': 'bar'})
        'risky fingerprint' payment_method.fingerprint = 'abcdeofgh101' return prevent 
        'risky card bin' card_bin in '046111', '014141' return prevent
    default allow
 end
```


### Basic Types
* String: Represent characters such as `'abc'` with quotes. 
* Numeric: Numbers with 2 decimals precision. 
* Boolean: A boolean can be `true` or `false` 
* List: Used to represent a collection of `String` elements from 1 to n elements. Example: `'foo', 'bar', 'foobar'`. For more information
about stored lists refer to the Stored Lists section below.  
* DateTime: A String representing date and time with ISO Basic Format. Example: `'2019-10-31T08:31:58.129'`
* Null Values: Whenever a null value is present, the condition result will be false.
#### Stored Lists
Rules Engine allows the user to declare and store bigger lists for rules evaluation. They must be activated in order to be 
accessed. Stored Lists can be used by comparing `String` values with the `list(<list name>)` operator. For example: 
``` 
workflow 'test'
    ruleset 'dummy'
        'rule_a' card_bin contains list('card_bins') return block
    default allow
end
```
The list operators: `contains`, `in` and `starts_with` are also available for stored lists. 

<em>Note: Stored lists must be activated before use.</em>

### Basic Operations

Basic operations allow to modify a given field and evaluate it in a logical expression. All mathematical functions 
will be rounded down to 2 decimals and the containing rule will be evaluated to false in the event of an error.

#### Math Operators 
* Addition `+`. Returns the sum of the left value with the right 
* Subtraction `-`. Returns the subtract 
* Multiplication `*`. Multiplies the left value by the right value. 
* Division `/`. Returns the division of the left value with the right value as the divisor. If the divisor is 0, 
the condition will be evaluated as `false`. The result will be scaled down to 2 decimals.

* Absolute value `abs`. Returns the absolute value of a number.

* Day Of Week `dayofweek`. Returns the day of the week value text of a date.

#### Logical Operators
`AND` Evaluates to true if all operands are true and not NULL, to false if one or more operands are false or null. 
     
`OR` When both operands are non null, the result is true if any operand is true, and false otherwise. With a null operand, the result is false if the other operand is false and false otherwise 
If both operands are false or null, the result is false.

#### List Operators
`contains` Returns true if any of the elements of the collection contains the given value

`in` Returns true if a value matches any of the elements of the collection

`startswith` Returns true if a value starts with any of the elements of the collection

##### The `not` Modifier

The `not` modifier can be used  to negate the result of the `contains`, `in`, and `startswith` operators. 

#### Date Operators
`datediff` Returns the difference between two dates in the specified unit: `day` `hour` or `minute`

`currentdate` Returns the current system date time.  

#### Date and DateTime Functions

The following functions are available for date and datetime operations:

* `date('YYYY-MM-DD')` — Casts a string or property to a date value.
* `datetime('YYYY-MM-DDTHH:MM[:SS][Z|±hh:mm]')` — Casts a string or property to a datetime value.
* `now()` — Returns the current system date and time as a datetime value.
* `date_add(date, amount, unit)` — Adds the specified amount of the given unit (`day`, `hour`, `minute`) to the date or datetime value.
* `date_subtract(date, amount, unit)` — Subtracts the specified amount of the given unit from the date or datetime value.
* `date_diff(unit, date1, date2)` — Returns the difference between two dates or datetimes in the specified unit (`day`, `hour`, `minute`).

**Examples:**
```text
'future_date' date_add(date('2024-06-01'), 5, day) = date('2024-06-06') return block
'past_date' date_subtract(datetime('2024-06-01T12:00Z'), 2, hour) = datetime('2024-06-01T10:00Z') return block
'now_check' now() > date('2024-06-01') return block
'diff_check' date_diff(day, date('2024-06-01'), date('2024-06-10')) = 9 return block
```

#### Aggregate Operators
`count` Returns the number of elements in a collection

`average` Returns the average value of elemens in the collection of 
numbers

`any` returns true if at least one element matches the given predicate.
`all` returns true if all elements match the given predicate. Note that all() returns true when called with any valid predicate on an empty collection.
`distinct` Returns the count of distinct non null values matching the given predicate

**Aggregation Predicates:**
The aggregation operators support two types of predicates:

1. **Property Comparisons** - Compare properties of list items:
```text
'property_match' order.items.any { type = 'restricted' } return block
'bulk_items' order.items.any { quantity > 100 } return review
```

2. **Direct Value Comparisons** - Compare list items directly to values:
```text
'string_match' customer.tags.any {'blocked'} return block
'numeric_match' user.scores.any {100} return high_score
'boolean_match' user.flags.any {true} return has_flag
```

### Returns and Tags
#### Return States
`allow`	The evaluation is not risky

`prevent` ake extra validation steps for this evaluation (e.g. put the customer through Identity Validation)

`block`	Block this order

##### Tags

Tags allow the result to communicate behaviour and information not covered with the risk. For example, a tag `not_risk_user`
can be used in the order creation evaluation.

#### Actions  
Actions allow the result of the risk evaluation to inform expected actions to be executed. For example in the order creation, 
the workflow result might contain the `manual_review` action, to notify the receiver that a manual review case must be created. 
`manual_review`. A rule can return zero, one or more actions using the `AND` connector. 

To identify a returning action use the `action(<action_name>, [<action_params>])`. The `action_name` is a String
identifier and the `<action_params>` parameter is an optional collection in JSON notation that allows storing additional configuration.
For example: 

```
 workflow 'test'
    ruleset 'dummy'
        'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'foo': 'bar'}) and action('logout_user')
    default allow
 end
```

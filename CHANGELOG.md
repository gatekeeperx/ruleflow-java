# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.19.0]

### Added
- **Async pre-resolution of custom functions**: slow custom functions (e.g. `screening`) can now be resolved ahead of evaluation instead of synchronously during it.
  - `Workflow.extractFunctionCalls(payload, lists)` returns every custom-function call site with its arguments already resolved against the payload, each carrying a canonical key (`PendingFunctionCall`).
  - `Workflow.evaluate(payload, lists, functions, resolvedFunctions)` injects a `key -> result` map; when a call's key is present, the pre-resolved result is used and the function is never invoked.
  - `RuleflowFunctionKey.of(functionName, resolvedArgs)` produces the deterministic, serializable key shared by both sides.
  - Fully backward compatible: existing `evaluate(...)` overloads and behavior are unchanged; a missing key falls back to invoking the function as before.

## [0.11.0]

### Added
- **New `evalInList` function**: Added a new function to evaluate predicates over list elements
  - Syntax: `evalInList('listName', predicate)`
  - Returns `true` if any element in the list matches the predicate, `false` otherwise
  - Example: `evalInList('blacklist', elem.field1 = 'test')`
  
- **New `elem` keyword**: Added a reserved keyword for referencing the current list element within `evalInList` predicates
  - Use `elem` to reference the current list item being evaluated
  - Use `elem.field1` to access properties of the current list item
  - Supports nested properties: `elem.field1.field2`
  - Example: `evalInList('users', elem.status = 'active' AND elem.score > 100)`

### Technical Details
- Added `EvalInListContextEvaluator` to handle the evaluation logic
- Implemented `ScopedVisitor` to provide scoped context for `elem` resolution within predicates
- Updated grammar (`RuleFlowLanguage.g4`) to support `evalInList` expression and `elem` keyword
- Added comprehensive test coverage with 17 test cases covering various scenarios:
  - Basic matching and non-matching cases
  - Nested property access
  - Comparison operators (equals, not equals, greater than, less than, etc.)
  - Logical operators (AND, OR)
  - String operations (contains)
  - Numeric comparisons
  - Edge cases (empty lists, missing lists)
  - Parent context access within predicates

### Changed
- Updated `ValidPropertyContextEvaluator` to handle `K_ELEM` tokens in property paths
- Modified visitor dispatch to properly handle `PropertyContext` and `ValidPropertyContext` in scoped contexts

## [0.10.2] - Previous Release
- Existing functionality


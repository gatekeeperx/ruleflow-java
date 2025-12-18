# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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


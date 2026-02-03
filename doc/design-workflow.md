# Design Workflow Guide

This document establishes our process for designing and implementing features in a test-driven, well-documented manner.

## Core Principles

1. **Test-First Development**: Write tests before implementation
2. **Single Source of Truth**: Constants and contracts defined once, used everywhere
3. **Documentation as Design**: Write behavior specifications before code
4. **Incremental Validation**: Test small pieces, build incrementally

---

## Workflow: Adding a New Feature

### Phase 1: Design Document (Before ANY code)

Create a design doc in `doc/design/` following this template:

```markdown
# Feature: [Feature Name]

## Purpose
What problem does this solve? Why do we need it?

## User Stories
- As a [user type], I want to [action] so that [benefit]

## API Contract
### Inputs
- Parameter names and types
- Valid ranges/constraints
- Default values

### Outputs
- Return type and structure
- Possible error states

### Side Effects
- What state changes?
- What gets logged/persisted?

## Examples
```clojure
;; Happy path
(my-function {:x 10 :y 20}) ;=> {:result "success"}

;; Edge cases
(my-function {:x 0 :y 0}) ;=> {:result "at-origin"}

;; Error cases
(my-function nil) ;=> throws IllegalArgumentException
```

## Dependencies
- What existing systems does this interact with?
- What constants/types does it use?

## Test Strategy
- What behaviors need testing?
- What edge cases exist?
- What error conditions must be handled?
```

### Phase 2: Write Test Structure (Still no implementation!)

Create the test file with test names and descriptions:

```clojure
(ns my-feature-test
  (:require [clojure.test :refer :all]
            [my-feature :as feature]))

;; =============================================================================
;; Basic Functionality Tests
;; =============================================================================

(deftest create-widget
  (testing "Create a widget with valid parameters"
    ;; TODO: implement assertions
    ))

(deftest create-widget-with-defaults
  (testing "Widget creation uses sensible defaults"
    ;; TODO: implement assertions
    ))

;; =============================================================================
;; Edge Cases
;; =============================================================================

(deftest handle-zero-coordinates
  (testing "Widget at origin behaves correctly"
    ;; TODO: implement assertions
    ))

;; =============================================================================
;; Error Handling
;; =============================================================================

(deftest reject-invalid-input
  (testing "Widget creation rejects nil parameters"
    ;; TODO: implement assertions
    ))
```

### Phase 3: Define Constants and Types

Add any new constants to `src/a_game/constants.clj`:

```clojure
(def WIDGET_MAX_SIZE 100)
(def WIDGET_TYPES #{:standard :enhanced :custom})
```

### Phase 4: Implement Tests (with actual assertions)

Fill in the test bodies with concrete assertions:

```clojure
(deftest create-widget
  (testing "Create a widget with valid parameters"
    (let [widget (feature/create-widget {:size 10 :type :standard})]
      (is (map? widget))
      (is (= 10 (:size widget)))
      (is (= :standard (:type widget))))))
```

**Run the tests - they should FAIL** (you haven't implemented anything yet!)

### Phase 5: Implement Minimum Code to Pass Tests

Write just enough code to make tests pass:

```clojure
(ns my-feature
  (:require [a-game.constants :as const]))

(defn create-widget
  "Create a new widget with the specified parameters."
  [{:keys [size type] :or {size 10 type :standard}}]
  {:size size
   :type type})
```

**Run tests again - they should PASS**

### Phase 6: Refactor and Document

- Add comprehensive docstrings
- Refactor for clarity
- Add usage examples to docstrings
- Run tests to ensure nothing broke

### Phase 7: Integration Testing

Test how this feature works with existing systems:

```clojure
(deftest widget-integrates-with-stage
  (testing "Widgets can be added to stage"
    (let [stage (env/create_stage "test" #{} #{} nil nil nil)
          widget (feature/create-widget {:size 10})]
      (env/add-entity stage widget)
      (is (contains? (:entities @stage) (:id widget))))))
```

---

## Documentation Locations

### 1. Design Documents (`doc/design/`)
- Initial feature specifications
- API contracts and examples
- Design decisions and tradeoffs

### 2. Architecture Decision Records (`doc/decisions/`)
- Why we chose approach X over Y
- Trade-offs considered
- Context and consequences

Example ADR structure:
```markdown
# ADR-001: Use Period (.) for Floor Tiles

## Status
Accepted

## Context
Generated maps need a visual character for floor tiles. Options were:
- Space character (invisible, hard to debug)
- Period (visible, clear in text editors)

## Decision
Use period (.) for floor tiles in all ASCII map representations.

## Consequences
- Maps are easier to debug visually
- Parser must recognize period as walkable
- Consistent with roguelike conventions
```

### 3. API Documentation (`wiki/API.md`)
- Public function signatures
- Usage examples
- Common patterns

### 4. Docstrings (in code)
- Function-level documentation
- Parameter descriptions
- Return value descriptions
- Usage examples

---

## Communication Protocol: Working Together

### When Starting Work

**You say:**
> "I want to add feature X that does Y"

**I'll ask:**
1. What's the expected input/output?
2. What constants/types are involved?
3. What are the edge cases?
4. What existing systems does it interact with?

**Together we:**
1. Write a design doc
2. Define test structure
3. Implement tests
4. Implement code
5. Validate and refactor

### When Changing Existing Code

**You say:**
> "Function X isn't working right, it should do Y instead"

**I'll do:**
1. Read existing tests to understand current contract
2. Check design docs for original intent
3. Propose test changes first
4. Then propose implementation changes
5. Run full test suite to check for regressions

### When Debugging

**You say:**
> "Tests are failing in module X"

**I'll do:**
1. Read the failing test to understand expectation
2. Check the implementation
3. Identify the mismatch
4. Propose fix (test or implementation)
5. Verify fix doesn't break other tests

---

## Quick Reference: File Organization

```
a_game/
├── doc/
│   ├── design/           # Feature specifications (WRITE FIRST)
│   │   └── collision-system.md
│   ├── decisions/        # Architecture Decision Records
│   │   └── adr-001-floor-tiles.md
│   └── intro.md
├── src/
│   └── a_game/
│       ├── constants.clj # SINGLE SOURCE OF TRUTH
│       ├── core.clj
│       └── [feature]/
│           └── feature.clj  # Implementation (WRITE LAST)
├── test/
│   └── [feature]/
│       └── feature_test.clj # Tests (WRITE SECOND)
└── wiki/
    ├── API.md           # Public API reference
    └── Getting-Started.md

```

---

## Example: Full Workflow

Let's say you want to add a "damage calculation" feature.

### Step 1: Create `doc/design/damage-calculation.md`
Define what damage calculation means, inputs (attacker, defender, weapon), outputs (damage value), edge cases (0 damage, critical hits, immunity).

### Step 2: Update `src/a_game/constants.clj`
```clojure
(def MIN_DAMAGE 0)
(def MAX_DAMAGE 999)
(def CRIT_MULTIPLIER 2.0)
```

### Step 3: Create `test/combat/damage_test.clj`
Write test names and empty bodies.

### Step 4: Fill in test assertions
```clojure
(deftest calculate-basic-damage
  (testing "Basic damage calculation"
    (is (= 10 (combat/calculate-damage {:power 10} {:defense 0})))))
```

### Step 5: Run tests (should fail)

### Step 6: Implement `src/combat/damage.clj`
```clojure
(defn calculate-damage [attacker defender]
  ...)
```

### Step 7: Run tests (should pass)

### Step 8: Add docstrings and examples

### Step 9: Update `wiki/API.md` with public functions

---

## Testing Checklist

Before marking a feature "complete":

- [ ] Design doc exists and is current
- [ ] Constants defined in `constants.clj`
- [ ] All tests passing
- [ ] Docstrings on all public functions
- [ ] Edge cases tested
- [ ] Error cases tested
- [ ] Integration tests added
- [ ] Wiki/API docs updated
- [ ] No regressions in other modules

---

This workflow ensures we always know:
1. **What** we're building (design doc)
2. **How** it should behave (tests)
3. **Why** it works this way (ADRs)
4. **How to use it** (docstrings + wiki)

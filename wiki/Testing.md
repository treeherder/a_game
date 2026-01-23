# Testing Guide

This document explains the test suite for the a_game project. Tests are organized by module and ensure the reliability of core game systems.

---

## Testing Philosophy

Our testing approach follows these principles:

- **Test Behavior, Not Implementation**: Tests validate what code does, not how it does it
- **Comprehensive Coverage**: Core systems have extensive tests; edge cases are explicitly covered
- **Documentation Through Tests**: Tests serve as executable documentation showing how systems work
- **Modular Design**: Tests mirror the component-based architecture of the game
- **Integration & Unit**: Both fine-grained unit tests and system integration tests are valued

---

## Quick Reference

### Common Commands
```bash
# Run all tests
lein test

# Run specific namespace
lein test actors.agent-collision-test

# Run single test
lein test :only actors.agent-collision-test/composite-body-collision-bounds

# Continuous testing
lein test-refresh
```

### Test File Structure
```
test/
├── actors/              # Agent/entity tests
├── components/          # Component system tests (collision, properties)
├── engine/              # Game loop, clock, input tests
├── stage/               # World/stage tests
├── stagebuilder/        # Map generation tests
└── testmaps/            # Generated test map files
```

### Key Test Suites
- **Collision System**: `components.collision-test`, `actors.agent-collision-test`
- **Map Generation**: `stagebuilder.generator-test`, `stagebuilder.connectivity-test`
- **Game Clock**: `engine.clock-test`
- **Stage/World**: `stage.environment-test`, `stage.apply-event-test`

---

## Table of Contents

- [Core Tests](#core-tests)
- [Actor Tests](#actor-tests)
- [Component Tests](#component-tests)
- [Engine Tests](#engine-tests)
- [Stage Tests](#stage-tests)
- [Stagebuilder Tests](#stagebuilder-tests)

---

## Core Tests

### `a_game/core_test.clj`

| Test | Description |
|------|-------------|
| `create_entity_list` | Verifies that entities can be created and added to a world map. Ensures that adding an entity increases the count and the result is a valid set. |

**Why it's important:** This test validates the fundamental entity creation system. Without working entity creation, no game objects (players, items, enemies) could exist in the world.

---

## Actor Tests

### `actors/agent_test.clj`

| Test | Description |
|------|-------------|
| `agent-pool-returns-seq` | Creates a stage, adds an agent, and verifies that the agent pool returns a proper sequence. |

**Why it's important:** Agents are the active entities in the game (players, NPCs, creatures). This test ensures agents can be properly added to a stage and retrieved, which is essential for game logic that needs to iterate over all actors.

### `actors/agent_collision_test.clj`

**Collision as Universal Body Component** - This test suite implements collision as the foundational component for all entities with physical bodies. It covers composite body systems, movement, fine-grained collision detection, and stage integration.

| Test | Description |
|------|-------------|
| `entity-with-body-requires-collision` | Verifies entities with physical bodies must have collision components. |
| `entity-without-collision-has-no-physicality` | Abstract entities without collision have no physical presence. |
| `body-parts-have-individual-collision` | Each body part (head, torso, arms) is an entity with its own collision bounds. |
| `composite-body-collision-bounds` | Composite bodies' main collision encompasses all parts. |
| `body-parts-tracked-by-parent` | Body parts reference their parent entity via `:attached-to`. |
| `move-body-updates-collision-position` | Moving an entity updates its collision position. |
| `move-composite-body` | Moving composite bodies moves all parts together. |
| `two-bodies-collide` | Detects when two bodies occupy overlapping space. |
| `bodies-at-different-positions-no-collision` | Distant bodies don't collide. |
| `adjacent-bodies-no-collision` | Side-by-side bodies don't overlap. |
| `fine-grained-body-part-collision` | Detects which specific body parts are colliding. |
| `add-body-to-stage` | Adds composite body entities to the game world. |
| `query-bodies-at-position` | Queries which entities occupy a tile position. |
| `different-sized-bodies` | Tests bodies with different collision bounds (1x1 vs 2x2). |
| `large-body-blocks-more-tiles` | Larger bodies occupy multiple tiles. |
| `ghost-has-position-but-no-solidity` | Non-solid entities have position but don't block movement. |
| `entities-must-overlap-to-interact` | Entities must have overlapping collision to interact. |

**Key Concepts:**
- **Body as Component System**: Bodies are composite structures with a core entity and parts (head, torso, limbs)
- **Collision as Foundation**: All physical entities use collision for position, bounds, and solidity
- **Sub-entity Architecture**: Body parts are individual entities with `:attached-to` parent references
- **Movement Propagation**: Moving composite bodies updates all part positions
- **Solid vs Passable**: Collision includes `:solid` flag for blocking vs trigger zones

**Helper Functions:**
- `create-body-part` - Creates body parts with collision and parent attachment
- `create-humanoid-body` - Factory for composite humanoid (head/torso/legs)
- `move-body-with-parts` - Moves composite body and all parts by delta
- `get-colliding-parts` - Finds specific parts involved in collision
- `create-large-creature` - Factory for 2x2 creatures
- `create-small-creature` - Factory for 1x1 creatures  
- `create-ghost-entity` - Factory for non-solid positioned entities

**Why it's important:** These tests establish collision as the universal basis for physical entities:
- Enables complex body systems with individual parts
- Supports fine-grained collision for hit detection
- Allows different body sizes and shapes
- Provides foundation for interactions (combat, triggers)
- Supports both solid and passable entities
- Integrates with the stage spatial system

---

## Component Tests

### `components/prop_test.clj`

| Test | Description |
|------|-------------|
| `create-and-get-properties` | Creates an entity with components (hp, name) and verifies the properties are stored and retrievable correctly. |
| `set-component-config` | Tests setting component configurations on an entity and ensures they persist. |

**Why it's important:** The component system is the backbone of entity properties. These tests ensure:
- Entities can have attributes like health points and names
- Properties can be read back after being set
- Component configurations are properly stored

### `components/collision_test.clj`

| Test | Description |
|------|-------------|
| `create-collision-component` | Creates a collision component with position and bounds. |
| `create-collision-with-defaults` | Verifies default values (1x1 size, solid=true). |
| `create-passable-collision` | Creates non-solid collision for triggers/zones. |
| `add-collision-to-agent` | Adds collision component to agent entities. |
| `add-collision-to-scenery` | Adds collision to walls/obstacles with properties. |
| `update-collision-position` | Updates entity position for movement. |
| `point-collision-check` | Checks if a point is within collision bounds. |
| `entity-overlap-check` | Detects when two entities' bounds overlap. |
| `passable-entities-dont-block` | Non-solid entities allow movement through. |
| `entities-with-collision-in-stage` | Integrates collision entities with stage. |
| `find-entities-at-position` | Queries all entities at a tile position. |
| `check-tile-walkable` | Verifies tile passability based on solid entities. |
| `wall-entity-creation` | Factory function for wall entities. |
| `floor-entity-creation` | Factory function for floor entities. |

**Why it's important:** Collision detection is essential for gameplay:
- Prevents entities from walking through walls
- Enables hit detection for combat
- Supports trigger zones for events
- Integrates with the stage for spatial queries

---

## Engine Tests

The engine module handles core game loop mechanics.

### `engine/clock_test.clj`

| Test | Description |
|------|-------------|
| `create-game-clock` | Creates a clock with initial tick count of 0. |
| `advance-single-tick` | Advances clock by one tick. |
| `advance-multiple-ticks` | Chains multiple tick advances. |
| `advance-n-ticks` | Advances by N ticks at once. |
| `clock-embedded-in-stage` | Attaches clock to a stage. |
| `tick-stage-clock` | Advances stage's embedded clock. |
| `schedule-future-event` | Schedules event for future tick. |
| `schedule-multiple-events` | Multiple events at different ticks. |
| `events-fire-at-correct-tick` | Events fire when their tick is reached. |
| `events-removed-after-firing` | Events removed from pending after firing. |
| `schedule-relative-event` | Schedule relative to current tick. |
| `default-tick-rate` | Default ticks-per-second is set. |
| `custom-tick-rate` | Custom tick rate configuration. |
| `convert-ticks-to-seconds` | Tick to time conversion. |
| `convert-seconds-to-ticks` | Time to tick conversion. |
| `pause-clock` | Pauses the clock. |
| `resume-clock` | Resumes a paused clock. |
| `tick-while-paused-does-nothing` | Paused clock doesn't advance. |
| `entity-action-cooldown` | Entity action cooldown in ticks. |
| `entity-ready-to-act` | Entity ready when cooldown expires. |
| `entity-not-ready-during-cooldown` | Entity blocked during cooldown. |
| `record-entity-action` | Records when entity acts. |
| `full-game-loop-tick` | Integration test for game loop. |

**Why it's important:** The clock/tick system is the heartbeat of the game:
- Enables turn-based or real-time gameplay
- Schedules timed events (spawns, effects, cooldowns)
- Controls entity action timing
- Provides pause/resume functionality
- Converts between game ticks and real time

---

## Stage Tests

The stage is the game world container that holds entities and agents.

### `stage/environment_test.clj`

| Test | Description |
|------|-------------|
| `stage` | Verifies that `create_stage` returns an atom containing expected keys (label, agent_pool, entities, stage_id). |
| `stage_map` | Tests that adding entities and agents properly updates the stage data structure. |

**Helper Functions:**
- `test_blade` - Creates a blade object with metal properties
- `scrap_wood` - Creates a wood object that is flammable
- `knife` - Creates a composite object from handle and blade

**Why it's important:** These tests validate the core stage/world container:
- Stages are created with proper structure
- Entities and agents can be added and tracked
- The stage maintains referential integrity

### `stage/apply_event_test.clj`

| Test | Description |
|------|-------------|
| `apply-event-add-entity` | Tests that committing an `:add-entity` event properly adds an entity to the stage. |
| `apply-event-add-agent` | Tests that committing an `:add-agent` event properly adds an agent to the agent pool. |

**Why it's important:** The event system is how game state changes happen. These tests ensure:
- Events are properly processed
- Stage state is correctly updated through events
- Event-based architecture functions correctly

### `stage/damage_event_test.clj`

*Placeholder file for future damage event tests.*

### `stage/event_serialization_test.clj`

*Placeholder file for future event serialization tests.*

---

## Stagebuilder Tests

The stagebuilder module handles procedural map generation and parsing.

### `stagebuilder/generator_test.clj`

| Test | Description |
|------|-------------|
| `generate-labyrinth-structure` | Generates an 8000x8000 labyrinth and verifies dimensions and minimum room count. |
| `room-detection` | Tests point-in-room detection logic for coordinates inside/outside a room. |
| `wall-detection` | Tests wall detection at room boundaries vs interior points. |
| `tile-rendering` | Verifies tiles are valid characters (walls `#` or floors ` `) and room interiors are walkable. |
| `render-small-labyrinth` | Tests rendering a 50x30 labyrinth to string output with correct dimensions. |

**Why it's important:** These tests validate the procedural generation engine:
- Large maps can be generated (up to 8000x8000)
- Rooms are correctly detected
- Walls and floors are properly disttinguished
- Rendering produces valid ASCII ouput

### `stagebuilder/parser_test.clj`

| Test | Description |
|------|-------------|
| `parse-ascii-map-basic` | Parses a simple ASCII map and verifies dimensions and tile types. |
| `find-walkable-and-walls` | Tests finding walkable tiles (spaces) and walls (`#`) from a parsed grid. |
| `grid-to-stage-conversion` | Converts a parsed grid to a stage structure with proper labels and tile sets. |
| `load-stage-from-generated-file` | End-to-end test: generates labyrinth → saves to file → reloads and validates. |

**Why it's important:** These tests ensure maps can be:
- Loaded from ASCII text files
- Parsed into game-usable data structures
- Converted to stage objects with walkable/wall tile sets
- Saved and loaded reliably

### `stagebuilder/connectivity_test.clj`

| Test | Description |
|------|-------------|
| `test-full-connectivity` | Uses flood-fill to verify all walkable tiles are connected in a 100x100 labyrinth. |
| `test-all-rooms-reachable` | Verifies every room center is reachable from every other room. |

**Helper Functions:**
- `get-all-walkable-tiles` - Collects all room interior + corridor tiles
- `get-neighbors` - Gets adjacent walkable tiles for pathfinding
- `flood-fill` - BFS algorithm to find all connected tiles

**Why it's important:** This is critical for gameplay:
- Mobile entities must be able to reach all areas
- No isolated/inaccessible rooms
- Corridor generation properly connects rooms

### `stagebuilder/full_labyrinth_test.clj`

| Test | Description |
|------|-------------|
| `generate-medium-labyrinth` | Generates a 1000x1000 labyrinth and validates structure (≥10 rooms, corridors exist). |
| `save-and-load-medium-labyrinth` | Full round-trip: generate → save → load → validate with detailed metrics. |
| `corridor-generation` | Verifies corridors exist and contain walkable (space) tiles. |

**Why it's important:** These are integration tests for medium-scale maps:
- Performance validation for 1000x1000 maps
- File I/O reliability
- Corridor system functionality

### `stagebuilder/map_generation_test.clj`

| Test | Description |
|------|-------------|
| `generate-five-random-maps-test` | Generates 5 random maps (50-100 tiles per dimension) to the `test/testmaps/` folder. |

**Why it's important:** This test:
- Validates random generation produces valid output
- Creates sample maps for visual inspection
- Tests file system operations

---

## Running Tests

### Run All Tests

Run the entire test suite:
```bash
lein test
```

### Run Specific Namespace

Run all tests in a specific namespace:
```bash
lein test actors.agent-collision-test
lein test components.collision-test
lein test stagebuilder.generator-test
```

### Run Single Test

Run a specific test function:
```bash
lein test :only actors.agent-collision-test/composite-body-collision-bounds
lein test :only stagebuilder.connectivity-test/test-full-connectivity
```

### Run Tests with Auto-reload

Use `lein-test-refresh` for continuous testing during development:
```bash
lein test-refresh
```

### Run Tests by Pattern

Run all tests matching a pattern:
```bash
# Run all collision-related tests
lein test :only '*collision*'

# Run all body-related tests  
lein test :only '*body*'
```

### Test Output

Successful test run:
```
lein test actors.agent-collision-test

Ran 18 tests containing 51 assertions.
0 failures, 0 errors.
```

Failed test example:
```
FAIL in (entity-with-body-requires-collision) (agent_collision_test.clj:42)
expected: (collision/has-collision? body-entity)
  actual: false
```

---

## Test Organization

### File Naming Conventions

- Test files use underscores: `agent_collision_test.clj`
- Namespace uses hyphens: `actors.agent-collision-test`
- Test names use lowercase with hyphens: `entity-with-body-requires-collision`

### Test Structure

```clojure
(ns module.feature-test
  (:require [clojure.test :refer :all]
            [module.feature :as feature]))

(deftest descriptive-test-name
  (testing "Human-readable description of what's being tested"
    (let [test-data (setup-test-data)]
      (is (= expected-value (function-under-test test-data)))
      (is (predicate? result)))))
```

### Helper Functions

Define helper functions within test files for:
- Creating test data
- Setting up complex scenarios
- Reducing duplication across tests

Example:
```clojure
(defn create-test-entity
  "Helper to create entities for testing."
  [x y]
  (-> (agent/_entity)
      (collision/add-collision
       (collision/create-collision {:x x :y y}))))

(deftest use-helper-function
  (testing "Using helper to create test data"
    (let [entity (create-test-entity 5 10)]
      (is (= 5 (:x (collision/get-collision entity)))))))
```

---

## Test Data

Sample test maps are stored in `test/testmaps/`:
- `random_map_1.txt` through `random_map_5.txt` - Auto-generated test maps

Resource maps in `resources/maps/`:
- `sample_maze.txt` - Sample labyrinth
- `large_labyrinth.txt` - Larger test map
- `demomap` - Demo level

---

## Adding New Tests

### Step-by-Step Guide

1. **Create the test file**
   ```bash
   # Use underscores in filename
   touch test/module_name/feature_name_test.clj
   ```

2. **Set up the namespace**
   ```clojure
   (ns module-name.feature-name-test
     (:require [clojure.test :refer :all]
               [module-name.feature :as feature]
               [other.dependencies :as deps]))
   ```

3. **Write your first test**
   ```clojure
   (deftest basic-functionality-test
     (testing "Basic feature works as expected"
       (let [result (feature/do-something 42)]
         (is (= expected-value result)))))
   ```

4. **Run the test**
   ```bash
   lein test module-name.feature-name-test
   ```

### Test Writing Best Practices

#### Use Descriptive Names

**Good:**
```clojure
(deftest entity-with-collision-component-is-solid)
(deftest composite-body-moves-all-parts-together)
```

**Bad:**
```clojure
(deftest test1)
(deftest it-works)
```

#### Include Testing Contexts

Use `testing` blocks to describe what each assertion validates:
```clojure
(deftest complex-feature-test
  (testing "Setup creates proper initial state"
    (is (map? initial-state))
    (is (contains? initial-state :entities)))
  
  (testing "Operation modifies state correctly"
    (is (= 2 (count (:entities modified-state)))))
  
  (testing "Cleanup removes resources"
    (is (empty? (:entities cleaned-state)))))
```

#### Test Edge Cases

Cover boundary conditions and error cases:
```clojure
(deftest bounds-checking
  (testing "Point at exact boundary is included"
    (is (collision/point-in-bounds? 5 5 entity)))
  
  (testing "Point one unit outside is excluded"
    (is (not (collision/point-in-bounds? 4 5 entity))))
  
  (testing "Negative coordinates work correctly"
    (is (collision/point-in-bounds? -1 -1 negative-entity))))
```

#### Use Let Bindings for Clarity

Structure tests with clear setup:
```clojure
(deftest collision-detection-test
  (testing "Two entities collide when overlapping"
    (let [entity-a (create-entity 0 0 2 2)
          entity-b (create-entity 1 1 2 2)
          entity-c (create-entity 10 10 1 1)]
      ;; These overlap
      (is (collision/overlaps? entity-a entity-b))
      ;; This doesn't overlap
      (is (not (collision/overlaps? entity-a entity-c))))))
```

#### Test One Concept Per Test

Keep tests focused on a single aspect:
```clojure
;; Good - tests one thing
(deftest entity-has-collision-component
  (let [entity (create-physical-entity)]
    (is (collision/has-collision? entity))))

;; Good - tests one thing
(deftest entity-collision-has-position
  (let [entity (create-physical-entity 5 10)]
    (is (= 5 (:x (collision/get-collision entity))))))

;; Bad - tests multiple unrelated things
(deftest entity-everything
  (let [entity (create-physical-entity)]
    (is (collision/has-collision? entity))
    (is (= "Bob" (:name entity)))
    (is (pos? (:health entity)))))
```

### Common Test Patterns

#### Factory Pattern for Test Data

Create helper functions to build test objects:
```clojure
(defn create-test-body
  "Factory for creating test bodies with collision."
  ([x y] (create-test-body x y 1 1))
  ([x y width height]
   (-> (agent/_entity)
       (assoc :type :test-body)
       (collision/add-collision
        (collision/create-collision {:x x :y y :width width :height height})))))

(deftest use-factory
  (let [body (create-test-body 5 5)]
    (is (collision/has-collision? body))))
```

#### Setup and Teardown

Use fixtures when tests need shared setup:
```clojure
(def test-stage (atom nil))

(defn stage-fixture [f]
  (reset! test-stage (env/create_stage "test" #{} #{} nil nil nil))
  (f)
  (reset! test-stage nil))

(use-fixtures :each stage-fixture)

(deftest stage-available
  (is (some? @test-stage)))
```

#### Testing Transformations

Verify before and after states:
```clojure
(deftest move-updates-position
  (let [entity (create-test-body 0 0)
        original-x (:x (collision/get-collision entity))
        moved (collision/set-position entity 10 15)
        new-x (:x (collision/get-collision moved))]
    (is (= 0 original-x))
    (is (= 10 new-x))
    (is (not= original-x new-x))))
```

#### Testing Collections

Verify collection properties:
```clojure
(deftest body-has-multiple-parts
  (let [body (create-humanoid-body 0 0)]
    ;; Count
    (is (= 3 (count (:parts body))))
    ;; Membership
    (is (every? collision/has-collision? (:parts body)))
    ;; Specific property
    (is (some #(= :head (:part-type %)) (:parts body)))))
```

### Integration Tests

Write tests that verify multiple systems work together:
```clojure
(deftest body-in-stage-collision-system
  (testing "Complete workflow: create body, add to stage, query position"
    (let [stage (env/create_stage "integration-test" #{} #{} nil nil nil)
          body (create-humanoid-body 10 10)]
      ;; Add to stage
      (env/add-agent stage (:core body))
      (doseq [part (:parts body)]
        (env/add-entity stage part))
      
      ;; Query by position
      (let [entities-at-10-10 (collision/entities-at-position @stage 10 10)]
        (is (pos? (count entities-at-10-10)))
        (is (some #(= (:id (:core body)) (:id %)) entities-at-10-10))))))
```

### Testing Error Conditions

Verify proper error handling:
```clojure
(deftest invalid-input-handling
  (testing "Nil entity returns false for collision check"
    (is (not (collision/has-collision? nil))))
  
  (testing "Entity without collision component returns nil bounds"
    (let [entity (agent/_entity)]
      (is (nil? (collision/get-bounds entity))))))
```

### Debugging Failed Tests

When a test fails:

1. **Check the assertion message**
   ```
   FAIL in (test-name) (file.clj:42)
   expected: (= 5 result)
     actual: (not (= 5 3))
   ```

2. **Add print statements**
   ```clojure
   (deftest debug-test
     (let [entity (create-test-body 5 5)]
       (println "Entity:" entity)
       (println "Collision:" (collision/get-collision entity))
       (is (= 5 (:x (collision/get-collision entity))))))
   ```

3. **Run single test in isolation**
   ```bash
   lein test :only namespace/specific-test
   ```

4. **Use REPL for investigation**
   ```clojure
   (require '[module.feature :as feature])
   (def test-data (create-test-data))
   (feature/function-under-test test-data)
   ```

### Test Checklist

Before committing new tests, verify:

- [ ] Tests have descriptive names
- [ ] Each test validates one specific behavior
- [ ] `testing` blocks explain what's being verified
- [ ] Edge cases are covered
- [ ] Tests are independent (don't rely on execution order)
- [ ] Helper functions are documented
- [ ] All tests pass locally
- [ ] Tests fail when expected behavior is removed (validates test works)

---

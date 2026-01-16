# Testing Guide

This document explains the test suite for the a_game project. Tests are organized by module and ensure the reliability of core game systems.

---

## Table of Contents

- [Core Tests](#core-tests)
- [Actor Tests](#actor-tests)
- [Component Tests](#component-tests)
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

### `actors/agent-tests.clj`

Contains design notes about entity physicality and body systems. Documents the planned architecture where entities can have complex body structures with limbs, appendages, and nested components.

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
- Walls and floors are properly distinguished
- Rendering produces valid ASCII output

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
- Players must be able to reach all areas
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

To run all tests:
```bash
lein test
```

To run a specific test namespace:
```bash
lein test stagebuilder.generator-test
```

To run tests with verbose output:
```bash
lein test :only stagebuilder.connectivity-test/test-full-connectivity
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

1. Create a new file in the appropriate `test/` subdirectory
2. Use the namespace convention: `module.feature-test`
3. Require `clojure.test` and the modules under test
4. Use `deftest` and `testing` macros for organization
5. Run tests to verify they pass

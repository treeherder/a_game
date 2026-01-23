# a_game — minimal entity-component stage

This repository is an early skeleton of an entity-component game system in Clojure.

Key concepts
- `stage` / world: a single root `atom` that stores the world map, agents, and entities.
- `entity`: lightweight map with `:id` and `:components`.
- `components`: small maps describing properties (HP, hardness, etc.).

## Map Generation

The project includes procedural labyrinth generation for creating large, explorable dungeons:

### Quick Start

```bash
# Generate a 100x100 labyrinth for testing
lein run -m a-game.mapgen resources/maps/sample_maze.txt 100 100

# Generate a larger 500x500 labyrinth
lein run -m a-game.mapgen resources/maps/medium.txt 500 500

# Generate with custom cell size (smaller = more rooms/obstacles)
lein run -m a-game.mapgen resources/maps/dense.txt 200 200 15

# Syntax: lein run -m a-game.mapgen <output-file> <width> <height> [cell-size]
```

### REPL Usage

```clojure
(require '[stagebuilder.generator :as gen])

;; Generate labyrinth data structure
(def my-labyrinth (gen/generate-labyrinth 100 100))

;; With custom cell size (default is 20)
(def my-labyrinth (gen/generate-labyrinth 100 100 15))

;; Save to file
(gen/save-labyrinth my-labyrinth "resources/maps/my_map.txt")

;; Or render to string for preview
(println (gen/render-labyrinth my-labyrinth))

;; Inspect the structure
(:rooms my-labyrinth)       ;; Vector of room maps
(:wall-blocks my-labyrinth) ;; Vector of wall block obstacles
(:corridors my-labyrinth)   ;; Set of corridor coordinates
(:width my-labyrinth)       ;; Map width
(:height my-labyrinth)      ;; Map height
```

### Generated Labyrinth Features

- **Grid-cell subdivision**: Map divided into cells (default 20x20) with rooms placed in ~50% of cells
- **Random room sizes**: Small, medium, and large rooms with random dimensions within cells
- **Wall blocks**: Solid impassable obstacles placed in ~80% of remaining cells (inverse of rooms)
- **Wilson's algorithm**: Unbiased spanning tree maze with clean, non-overlapping corridors
- **Minimum spanning tree connections**: All rooms connected via L-shaped corridors
- **Full connectivity**: Every room and corridor guaranteed reachable
- **Border exits**: 1-3 random exits on map edges connected to the maze
- ASCII format: `#` for walls, `.` for walkable floor/corridors

**Important:** View generated map files with a **monospace font** (e.g., Courier, Consolas, Monaco).

### Generation Algorithm

1. **Cell Subdivision**: Map divided into grid cells (configurable size)
2. **Room Placement**: ~50% of cells receive randomly-sized rooms
3. **Wall Block Placement**: ~80% of remaining cells get solid wall obstacles
4. **Room Connection**: MST algorithm connects all rooms with L-shaped corridors
5. **Wilson's Algorithm**: Carves unbiased maze passages around obstacles
6. **Border Exits**: 1-3 random exits added to map edges

Example: A 100x100 map generates ~4,000 walkable tiles with 12 rooms in ~1-3 seconds.

Primary API (in `src/stage/environment.clj`)
- `create_stage(label, agent_pool, item_pool, mapfile, coordgrids, rules)` → returns an atom containing the stage map
- `add-entity(world-atom, entity)` → adds entity into `:entities`
- `add-agent(world-atom, agent)` → adds agent into `:agent_pool`
- `get-agents(world)` → returns a seq of agents (accepts atom or plain map)
- `apply-event(world, event)` → pure function applying an event to a plain map
- `commit-event(world-atom, event)` → swaps an atom with `apply-event`
- `snapshot(world)` → deref an atom world and return plain map

Component helpers (in `src/components/properties.clj`)
- `create_entity :entity <entity> :components <component-map>` → returns an updated entity with components attached
- `get_properties(entity)` → returns the `:components` collection
- `set_component_configuration(entity, changes)` → returns updated entity with changed components

Agents (in `src/actors/agent.clj`)
- `/_entity` → create a new entity with a UUID and empty components
- `agent_pool(stage)` → wrapper around `get-agents`

Examples

Create a stage and add an agent:

```clojure
(require '[stage.environment :as env] '[actors.agent :as a])
(def st (env/create_stage "demo" #{} #{} nil nil nil))
(def agent (a/_entity))
(env/add-agent st agent)
@st ; => snapshot of the world with the agent in :agent_pool
```

Apply an event (pure) and commit it to the atom:

```clojure
(def event {:type :add-entity :entity (a/_entity)})
(env/commit-event st event)
```

## Testing

Run the test suite with Leiningen:

```bash
# Run all tests
lein test

# Run specific test namespace
lein test actors.agent-collision-test
lein test components.collision-test

# Run a single test
lein test :only actors.agent-collision-test/composite-body-collision-bounds
```

### Test Coverage

The project has comprehensive test coverage across all major systems:

- **Collision System** (18 tests, 51 assertions): Tests collision as the universal basis for physical entities, including composite bodies, movement, and stage integration
- **Map Generation** (10+ tests): Validates procedural labyrinth generation, connectivity, room detection, and file I/O
- **Game Clock** (19 tests): Tests tick system, event scheduling, pause/resume, and cooldowns
- **Stage/World** (8+ tests): Tests entity management, agent pools, and event processing
- **Components** (14+ tests): Tests property system and collision components

**See [wiki/Testing.md](wiki/Testing.md) for complete testing documentation**, including:
- Detailed test descriptions and rationale
- Test writing best practices
- Examples and patterns
- Debugging guide

### Quick Test Examples

```clojure
;; Run in REPL to experiment with tested functionality
(require '[actors.agent :as a])
(require '[components.collision :as collision])

;; Create an entity with collision (physical body)
(def player (-> (a/_entity)
                (collision/add-collision
                 (collision/create-collision {:x 10 :y 20 :solid true}))))

;; Check collision properties
(collision/has-collision? player)  ;; => true
(collision/get-bounds player)      ;; => {:x 10 :y 20 :width 1 :height 1}
(collision/solid? player)          ;; => true

;; Move the entity
(def moved-player (collision/set-position player 15 25))
```

Notes
- This codebase intentionally uses a single root `atom` for the world to make snapshotting and serverless multiplayer easier. If you find performance hotspots, extract only the hot collection to its own `atom` and adapt `snapshot` accordingly.

# Feel free to add more tests and event handlers as the game logic grows.

# Wiki
# ----
# We maintain a small project wiki in the `wiki/` folder. See `wiki/Home.md` for navigation.
# a_game

a_game is an extensible, scaling component-entity system for a multiplayer hack-and-slash roguelike arena game.


##### Installation

Grab a binary from the releases page!

## Usage

Roll up a character, join a party, and fight for glory!

    $ java -jar a_game-0.0.1-standalone.jar [obvious args here]

## Options

There are not many options.

## Examples
In order to create an entity:
  ```a-game.core=> (def foo (actors.agent/_entity))
     #'a-game.core/foo
     a-game.core=> foo
     ["a91cd14b-a686-48eb-945f-7d2f8b4aef85" #{}]
     a-game.core=> (def a_po #{foo, bar})
     #'a-game.core/a_po
     a-game.core=> a_po
     #{["a91cd14b-a686-48eb-945f-7d2f8b4aef85" #{}] ["6852888c-7c28-471c-b1ea-537502106aa7" #{}]}
```

...

### Bugs
- it doesn't work.
...

## License

Copyright © 2017 Brendan Reddy-Best

Distributed under the Eclipse Public License either version 1.0 or any later version.

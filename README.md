# a_game — minimal entity-component stage

This repository is an early skeleton of an entity-component game system in Clojure.

Key concepts
- `stage` / world: a single root `atom` that stores the world map, agents, and entities.
- `entity`: lightweight map with `:id` and `:components`.
- `components`: small maps describing properties (HP, hardness, etc.).

## Map Generation

The project includes procedural labyrinth generation for creating large, explorable dungeons:

```bash
# Generate a 100x100 labyrinth for testing
lein run -m a-game.mapgen resources/maps/sample_maze.txt 100 100

# Generate a large 2000x2000 labyrinth (recommended for gameplay)
lein run -m a-game.mapgen resources/maps/large_labyrinth.txt 2000 2000

# Generate a custom-sized labyrinth
lein run -m a-game.mapgen <output-file> <width> <height> [cell-size]
```

Generated labyrinths feature:
- **Grid-cell subdivision**: Map divided into cells (default 20x20) with rooms placed in ~50% of cells
- **Random room sizes**: Small, medium, and large rooms with random dimensions and positions within cells
- **Minimum spanning tree connections**: All rooms connected via direct L-shaped corridors
- **Extensive maze network**: Recursive backtracking algorithm carves complex maze passages throughout empty space
- **Full connectivity**: Every room and corridor is guaranteed reachable from every other location
- **Border exits**: 1-3 random exits placed on map edges
- **High traversable area**: ~50-60% walkable space with many branching paths and dead ends for exploration
- ASCII format: `#` for walls, ` ` (space) for walkable floor/corridors

**Important:** View generated map files with a **monospace font** (e.g., Courier, Consolas, Monaco) 
to ensure all tiles display with equal width and height dimensions.

### Generation Algorithm

The labyrinth generator uses a sophisticated multi-phase approach:

1. **Cell Subdivision**: Map divided into grid cells (configurable size, default 20x20)
2. **Room Placement**: Approximately 50% of cells receive randomly-sized rooms (30-90% of cell size)
3. **Room Connection**: Minimum spanning tree algorithm connects all rooms with L-shaped corridors
4. **Maze Carving**: Recursive backtracking carves extensive maze passages in remaining space
5. **Connectivity Guarantee**: Maze carving starts from existing corridors, ensuring full connectivity
6. **Border Exits**: 1-3 random exits added to map edges

Example: A 100x100 map generates ~5,800 walkable tiles with 12 rooms and complex maze passages in ~480ms.

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

Testing

Run the test suite with Leiningen:

```bash
lein test
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

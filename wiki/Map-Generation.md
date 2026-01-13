# Map Generation Guide

The `stagebuilder` module provides procedural generation of large, explorable labyrinths for roguelike gameplay.

## Quick Reference

```bash
# Generate a labyrinth
lein run -m a-game.mapgen <output-file> <width> <height> [cell-size]

# Examples
lein run -m a-game.mapgen resources/maps/small.txt 100 100
lein run -m a-game.mapgen resources/maps/medium.txt 1000 1000
lein run -m a-game.mapgen resources/maps/large.txt 2000 2000
lein run -m a-game.mapgen resources/maps/custom.txt 500 500 15
```

## Algorithm Overview

The labyrinth generator uses a multi-phase approach designed to create engaging, explorable dungeons:

### Phase 1: Cell Subdivision

The map is divided into a grid of cells (default 20x20 pixels each):

```clojure
(subdivide-into-cells width height cell-size)
;; Returns: [{:x 0 :y 0 :width 20 :height 20} ...]
```

### Phase 2: Room Placement

Approximately 50% of cells receive randomly-sized rooms:

- **Room sizes**: Between 30% and 90% of cell dimensions
- **Padding**: Minimum 2-pixel border within each cell
- **Random positioning**: Rooms randomly placed within their cells
- **Size variety**: Creates visual and gameplay interest

```clojure
(generate-rooms-in-cells cells)
;; Returns: [{:x 5 :y 7 :width 12 :height 14} ...]
```

### Phase 3: Room Connection

All rooms are connected using a **minimum spanning tree** algorithm:

- Iteratively connects nearest unconnected room to the existing network
- Uses L-shaped corridors (horizontal then vertical segments)
- Guarantees all rooms are reachable
- Minimizes unnecessary corridor overlap

```clojure
(connect-all-rooms rooms)
;; Returns: #{[x1 y1] [x2 y2] ...} ; set of corridor coordinates
```

### Phase 4: Maze Carving

A **recursive backtracking** algorithm carves extensive maze passages:

- Operates on odd coordinates (creates traditional maze grid)
- Respects room boundaries (rooms act as obstacles)
- Starts from points adjacent to existing corridors (ensures connectivity)
- Creates branching paths, dead ends, and loops
- Maximizes explorable space (~50-60% walkable)

```clojure
(carve-maze-in-empty-space width height rooms room-corridors)
;; Returns: #{[x1 y1] [x2 y2] ...} ; set of maze path coordinates
```

### Phase 5: Border Exits

1-3 random exits are added to map edges:

- Randomly placed on top, bottom, left, or right borders
- Creates 3-tile deep entrance/exit passages
- Provides entry points and objectives

```clojure
(add-border-exits width height rooms corridors)
;; Returns: [[x1 y1] [x2 y2] ...] ; exit coordinates
```

## ASCII Format

Generated maps use ASCII characters:

- `#` - Wall (impassable)
- ` ` (space) - Floor/corridor (walkable)

**Critical**: View maps with **monospace fonts only** (Courier, Consolas, Monaco, etc.)

## Loading Generated Maps

Use the `stagebuilder.parser` namespace to load ASCII maps:

```clojure
(require '[stagebuilder.parser :as parser])

;; Load from file
(def stage-data (parser/load-stage-from-file 
                 "resources/maps/large_labyrinth.txt"
                 "arena-1"))

;; Parse from string
(def grid (parser/parse-ascii-map ascii-string))
(def walkable (parser/find-walkable-tiles grid))
(def stage (parser/grid->stage grid "my-stage"))
```

The parsed stage contains:

```clojure
{:label "arena-1"
 :width 2000
 :height 2000
 :walkable #{[x1 y1] [x2 y2] ...}  ; set of walkable coordinates
 :walls #{[x1 y1] [x2 y2] ...}}     ; set of wall coordinates
```

## Performance

Generation times (approximate):

| Map Size | Rooms | Corridors | Time  |
|----------|-------|-----------|-------|
| 100x100  | 12    | 5,800     | 480ms |
| 1000x1000| 1,250 | 580,000   | ~30s  |
| 2000x2000| 5,000 | 2,300,000 | ~2m   |

## Customization

### Adjusting Cell Size

```bash
# Smaller cells = more rooms
lein run -m a-game.mapgen output.txt 500 500 10

# Larger cells = fewer, larger rooms
lein run -m a-game.mapgen output.txt 500 500 30
```

### In Code

```clojure
(require '[stagebuilder.generator :as gen])

;; Generate with custom parameters
(def labyrinth (gen/generate-labyrinth 800 600 25))

;; Access components
(:rooms labyrinth)      ;; Vector of room maps
(:corridors labyrinth)  ;; Set of corridor coordinates
(:width labyrinth)      ;; Map width
(:height labyrinth)     ;; Map height

;; Save to file
(gen/save-labyrinth labyrinth "custom_map.txt")
```

## Connectivity Guarantees

The generator guarantees full connectivity:

1. **Minimum spanning tree** ensures all rooms connect
2. **Maze carving starts from corridors** to avoid isolated sections
3. **Flood-fill tests** verify every tile is reachable

See `test/stagebuilder/connectivity_test.clj` for validation logic.

## Design Goals

- **High traversable area** (50-60%) for engaging exploration
- **Complex topology** with branching paths and dead ends
- **Room variety** with random sizes and positions
- **Guaranteed connectivity** - no unreachable areas
- **Performance** - large maps generate in reasonable time
- **Deterministic** - same seed produces same map (use `(rand-seed n)`)

## Troubleshooting

**Problem**: Map looks garbled or misaligned

**Solution**: Use a monospace font. In most editors:
- VS Code: Set `"editor.fontFamily": "Courier New"`
- Terminal: Use Courier, Consolas, or Monaco

**Problem**: Generation takes too long

**Solution**: Reduce map size or increase cell size to reduce room count

**Problem**: Not enough/too much open space

**Solution**: 
- More open space: Decrease cell size (more rooms/corridors)
- Less open space: Increase cell size (fewer, larger rooms)

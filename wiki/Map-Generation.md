# Map Generation Guide

The `stagebuilder` module provides procedural generation of large, explorable labyrinths for roguelike gameplay.

## Quick Start

### Command Line

```bash
# Generate a labyrinth
lein run -m a-game.mapgen <output-file> <width> <height> [cell-size]

# Examples
lein run -m a-game.mapgen resources/maps/small.txt 100 100
lein run -m a-game.mapgen resources/maps/medium.txt 500 500
lein run -m a-game.mapgen resources/maps/large.txt 1000 1000
lein run -m a-game.mapgen resources/maps/dense.txt 200 200 15  # smaller cells = more rooms
```

### REPL Usage

```clojure
(require '[stagebuilder.generator :as gen])

;; Generate the labyrinth data structure
(def my-labyrinth (gen/generate-labyrinth 100 100))

;; Or with custom cell size (default is 20)
(def my-labyrinth (gen/generate-labyrinth 100 100 15))

;; Save to file
(gen/save-labyrinth my-labyrinth "resources/maps/my_map.txt")

;; Or just render to string (for preview)
(println (gen/render-labyrinth my-labyrinth))

;; Inspect the generated structure
(:rooms my-labyrinth)       ;; Vector of room maps {:x :y :width :height :type}
(:wall-blocks my-labyrinth) ;; Vector of wall block obstacles
(:corridors my-labyrinth)   ;; Set of [x y] corridor coordinates
(:width my-labyrinth)       ;; Map width in tiles
(:height my-labyrinth)      ;; Map height in tiles
```

## Algorithm Overview

The labyrinth generator uses a multi-phase approach designed to create engaging, explorable dungeons:

### Phase 1: Cell Subdivision

The map is divided into a grid of cells (default 20x20 tiles each):

```clojure
(gen/subdivide-into-cells width height cell-size)
;; Returns: [{:x 0 :y 0 :width 20 :height 20} ...]
```

### Phase 2: Room Placement

Approximately 50% of cells receive randomly-sized rooms:

- **Room sizes**: Between 30% and 90% of cell dimensions
- **Padding**: Minimum 3-tile border within each cell
- **Random positioning**: Rooms randomly placed within their cells
- **Size variety**: Creates visual and gameplay interest

```clojure
(gen/generate-rooms-in-cells cells)
;; Returns: [{:x 5 :y 7 :width 12 :height 14 :type :room} ...]
```

### Phase 3: Wall Block Placement

~80% of remaining empty cells receive solid wall blocks (inverse of rooms):

- **Concentrated obstacles**: Force maze to wind around them
- **Smaller than rooms**: 30-60% of cell size
- **Creates chokepoints**: Makes navigation more challenging

```clojure
(gen/generate-wall-blocks-in-cells cells rooms)
;; Returns: [{:x 42 :y 63 :width 8 :height 6 :type :wall-block} ...]
```

### Phase 4: Room Connection

All rooms connected using a **minimum spanning tree** algorithm:

- Iteratively connects nearest unconnected room to the network
- Uses L-shaped corridors (horizontal then vertical segments)
- Guarantees all rooms are reachable
- Minimizes unnecessary corridor overlap

```clojure
(gen/connect-all-rooms rooms)
;; Returns: #{[x1 y1] [x2 y2] ...} ; set of corridor coordinates
```

### Phase 5: Wilson's Algorithm Maze

**Wilson's algorithm** carves an unbiased spanning tree maze:

- Creates clean, non-overlapping single-width corridors
- Controlled intersections (no merged/overlapping areas)
- Loop-erased random walks ensure uniform distribution
- Respects room and wall block boundaries
- Long winding passages with meaningful dead ends

```clojure
(gen/wilsons-algorithm width height rooms wall-blocks room-corridors)
;; Returns: #{[x1 y1] [x2 y2] ...} ; set of maze path coordinates
```

### Phase 6: Border Exits

1-3 random exits added to map edges, each connected to the maze:

- Randomly placed on top, bottom, left, or right borders
- L-shaped corridor connects exit to nearest walkable area
- Provides entry/exit points for gameplay

```clojure
(add-border-exits width height rooms corridors)
;; Returns: [[x1 y1] [x2 y2] ...] ; exit coordinates
```

## ASCII Format

Generated maps use ASCII characters:

- `#` - Wall (impassable)
- `.` - Floor/corridor (walkable)

**Critical**: View maps with **monospace fonts only** (Courier, Consolas, Monaco, etc.)

## Loading Generated Maps

Use the `stagebuilder.parser` namespace to load ASCII maps:

```clojure
(require '[stagebuilder.parser :as parser])

;; Load from file
(def stage-data (parser/load-stage-from-file 
                 "resources/maps/my_map.txt"
                 "arena-1"))

;; Parse from string
(def grid (parser/parse-ascii-map ascii-string))
(def walkable (parser/find-walkable-tiles grid))
(def stage (parser/grid->stage grid "my-stage"))
```

The parsed stage contains:

```clojure
{:label "arena-1"
 :width 100
 :height 100
 :walkable #{[x1 y1] [x2 y2] ...}  ; set of walkable coordinates
 :walls #{[x1 y1] [x2 y2] ...}}    ; set of wall coordinates
```

## Performance

Generation times (approximate, using Wilson's algorithm):

| Map Size  | Rooms | Wall Blocks | Corridors | Time   |
|-----------|-------|-------------|-----------|--------|
| 100x100   | 12    | 10          | ~4,000    | 1-3s   |
| 500x500   | 300   | 250         | ~100,000  | ~30s   |
| 1000x1000 | 1,250 | 1,000       | ~400,000  | ~2m    |

## Customization

### Adjusting Cell Size

```bash
# Smaller cells = more rooms and obstacles
lein run -m a-game.mapgen output.txt 500 500 10

# Larger cells = fewer, larger rooms
lein run -m a-game.mapgen output.txt 500 500 30
```

### In Code

```clojure
(require '[stagebuilder.generator :as gen])

;; Generate with custom parameters
(def labyrinth (gen/generate-labyrinth 200 200 15))

;; Access components
(:rooms labyrinth)       ;; Vector of room maps
(:wall-blocks labyrinth) ;; Vector of wall block obstacles
(:corridors labyrinth)   ;; Set of corridor coordinates
(:width labyrinth)       ;; Map width
(:height labyrinth)      ;; Map height

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

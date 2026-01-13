# Getting Started

## Installation

1. Install [Leiningen](https://leiningen.org/) (Clojure build tool)
2. Clone this repository
3. Run `lein test` to validate the project

## Quick Start

### Generate Your First Labyrinth

```bash
# Generate a small test labyrinth
lein run -m a-game.mapgen resources/maps/test.txt 100 100

# View the generated map (use a monospace font!)
cat resources/maps/test.txt
```

### Load and Use a Map

```clojure
(require '[stagebuilder.parser :as parser])
(require '[stage.environment :as env])

;; Load an ASCII map file
(def stage-data (parser/load-stage-from-file 
                 "resources/maps/test.txt" 
                 "test-stage"))

;; Create a stage with the loaded map
(def my-stage (env/create_stage 
               "test-stage" 
               #{} ;; agent pool
               #{} ;; item pool
               stage-data
               nil ;; coordgrids
               nil)) ;; rules
```

### Explore the Core APIs

- **Stage Management**: `src/stage/environment.clj` - World state, entities, events
- **Components**: `src/components/properties.clj` - Entity properties and configuration
- **Agents**: `src/actors/agent.clj` - Agent creation and management
- **Map Generation**: `src/stagebuilder/generator.clj` - Procedural labyrinth generation
- **Map Parsing**: `src/stagebuilder/parser.clj` - ASCII map file loading

## Running Tests

```bash
# Run all tests
lein test

# Run specific test namespace
lein test stagebuilder.connectivity-test

# Run with detailed output
lein test :verbose
```

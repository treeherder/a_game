# Changelog

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.0.2] - 2026-01-13

### Added
- **Procedural Labyrinth Generation**: New `stagebuilder.generator` namespace for creating large, explorable dungeons
  - Grid-cell subdivision algorithm for structured room placement
  - Random room sizing (small, medium, large) with ~50% cell occupancy
  - Minimum spanning tree room connections with L-shaped corridors
  - Recursive backtracking maze algorithm carving complex passages in empty space
  - Guaranteed full connectivity between all rooms and corridors
  - 1-3 random border exits on map edges
  - High traversable area (~50-60%) with many branching paths and dead ends
- **ASCII Map Parser**: New `stagebuilder.parser` namespace for loading ASCII map files
  - Support for space character as walkable floor tiles
  - `parse-ascii-map`, `find-walkable-tiles`, `grid->stage` functions
  - `load-stage-from-file` for complete file-to-stage pipeline
- **Map Generation CLI**: `a-game.mapgen` namespace for command-line map generation
- **Comprehensive Testing**:
  - `stagebuilder.generator-test`: Tests for labyrinth structure, dimensions, room placement
  - `stagebuilder.parser-test`: Tests for ASCII map parsing and conversion
  - `stagebuilder.full-labyrinth-test`: Integration tests for medium (1k x 1k) and large maps
  - `stagebuilder.connectivity-test`: Flood-fill algorithm verifying full connectivity
  - All tests pass consistently with 0 failures

### Changed
- Updated ASCII map format to use space character instead of `.` for walkable tiles
- Modified labyrinth generation to prioritize connectivity while maximizing explorable space
- Enhanced documentation with detailed generation algorithm explanation

## [0.0.1] - 2017-08-05

### Added
- Added single-root `atom` world representation and helpers in `src/stage/environment.clj`
- Implemented entity creation and component helpers in `src/components/properties.clj`
- Added event application and `:damage` event handling
- Added JSON serialization helpers for events
- Added tests for components, stages, events, and serialization
- Added documentation and wiki stubs
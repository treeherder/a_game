# ADR-001: Use Period (.) for Floor Tiles in ASCII Maps

## Status
**Accepted** - 2026-02-02

## Context

When generating and parsing ASCII maps, we need to represent different tile types with distinct characters. The two main types are:
- Walls (blocking movement)
- Floors (walkable areas)

For floor tiles, we had two primary options:

### Option 1: Space Character (` `)
**Pros:**
- Traditional in some roguelike games
- Creates more visual "emptiness" in maps
- Lighter visual weight

**Cons:**
- Invisible in most text editors
- Hard to distinguish from actual empty space
- Difficult to debug (can't see if floor exists or space is missing)
- Copy/paste errors are hard to spot
- Line endings and whitespace trimming cause issues

### Option 2: Period Character (`.`)
**Pros:**
- Visible in all text editors and terminals
- Easy to debug and verify map generation
- Standard in many roguelikes (NetHack, DCSS, etc.)
- Clear distinction from walls (`#`)
- Resistant to whitespace handling issues
- Works well in monospace fonts

**Cons:**
- Slightly more visual "noise"
- Takes up more visual weight than space

## Decision

**We will use period (`.`) for all floor/walkable tiles in ASCII map representations.**

This decision applies to:
- Generated maps from `stagebuilder.generator`
- Parsed maps in `stagebuilder.parser`
- Sample/test maps
- Saved map files

The constant `FLOOR_CHAR` defined in `a-game.constants` is set to `\.`

## Consequences

### Positive
- Maps are immediately readable and debuggable
- Visual verification of map generation works well
- Tests can easily check for floor tile presence
- No issues with text editor whitespace handling
- Consistent with roguelike genre conventions

### Negative
- Maps have more visual density (acceptable trade-off)

### Implementation Changes Required
1. ✅ Updated `stagebuilder.generator` to output `.` (already done)
2. ✅ Updated `stagebuilder.parser` to recognize `.` as walkable
3. ✅ Updated all tests to expect `.`
4. ✅ Created constants file with `FLOOR_CHAR` definition
5. ✅ Updated sample maps in tests

## References
- Test suite: All 140 tests passing as of 2026-02-02
- Constants: `src/a_game/constants.clj`
- Generator: `src/stagebuilder/generator.clj`
- Parser: `src/stagebuilder/parser.clj`

## Notes

The parser still supports both `.` and space for backward compatibility with any existing maps, but all new maps should use period exclusively.

Space character (`\space`) is now reserved for `VOID_CHAR` - representing uninitialized/unreachable areas outside the playable map bounds.

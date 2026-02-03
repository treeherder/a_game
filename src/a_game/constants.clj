(ns a-game.constants
  "Central constants file for tile characters, entity types, and game configuration.
   
   This file serves as the single source of truth for all game constants.
   Any code that needs to reference tile characters, entity types, or other
   game-wide constants should require this namespace.
   
   Usage:
   (require '[a-game.constants :as const])
   (= char const/FLOOR_CHAR)")

;; =============================================================================
;; Tile Character Representations
;; =============================================================================

(def WALL_CHAR
  "Character representing solid walls in ASCII maps."
  \#)

(def FLOOR_CHAR
  "Character representing walkable floor/corridor tiles in ASCII maps.
   Period (.) is used for better visibility in text files."
  \.)

(def VOID_CHAR
  "Character representing empty/uninitialized space outside playable areas."
  \space)

;; =============================================================================
;; Entity Character Representations (for rendering)
;; =============================================================================

(def PLAYER_CHAR
  "Character representing player entities in rendered output."
  \@)

(def ENEMY_CHAR
  "Character representing enemy entities in rendered output."
  \E)

(def ITEM_CHAR
  "Character representing items in rendered output."
  \i)

(def DOOR_CHAR
  "Character representing doors in rendered output."
  \+)

(def TRIGGER_CHAR
  "Character representing trigger zones (usually invisible/passable)."
  \T)

;; =============================================================================
;; Entity Types
;; =============================================================================

(def ENTITY_TYPES
  "Set of valid entity type keywords."
  #{:wall :floor :door :trigger :chest :player :enemy :item})

;; =============================================================================
;; Collision Properties
;; =============================================================================

(def SOLID
  "Entity blocks movement."
  true)

(def PASSABLE
  "Entity does not block movement."
  false)

;; =============================================================================
;; Map Generation Defaults
;; =============================================================================

(def DEFAULT_CELL_SIZE
  "Default cell size for map subdivision during generation."
  20)

(def DEFAULT_ROOM_DENSITY
  "Default percentage of cells that will contain rooms (0.0 - 1.0)."
  0.5)

(def MIN_ROOM_SIZE
  "Minimum dimension for generated rooms."
  4)

(def MAX_BORDER_EXITS
  "Maximum number of exits on map borders."
  3)

;; =============================================================================
;; Coordinate System
;; =============================================================================

(def DIRECTIONS
  "Cardinal directions for movement as [dx dy] vectors."
  {:north [0 -1]
   :south [0 1]
   :east  [1 0]
   :west  [-1 0]})

(def DIAGONAL_DIRECTIONS
  "Diagonal directions for movement as [dx dy] vectors."
  {:northeast [1 -1]
   :northwest [-1 -1]
   :southeast [1 1]
   :southwest [-1 1]})

(def ALL_DIRECTIONS
  "All eight directions (cardinal + diagonal)."
  (merge DIRECTIONS DIAGONAL_DIRECTIONS))

;; =============================================================================
;; Validation Helpers
;; =============================================================================

(defn valid-entity-type?
  "Check if a keyword is a valid entity type."
  [entity-type]
  (contains? ENTITY_TYPES entity-type))

(defn valid-tile-char?
  "Check if a character is a valid tile character."
  [c]
  (or (= c WALL_CHAR)
      (= c FLOOR_CHAR)
      (= c VOID_CHAR)))

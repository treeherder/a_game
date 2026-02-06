(ns engine.movement
  "Movement system for entities with collision detection.
   
   Handles:
   - Direction-based movement (up, down, left, right)
   - Collision checking against stage walls and entities
   - Position updates"
  (:require [components.collision :as collision])
  (:gen-class))

;; =============================================================================
;; Direction Definitions
;; =============================================================================

(def directions
  "Movement directions as [dx dy] vectors."
  {:up        [0 -1]
   :down      [0 1]
   :left      [-1 0]
   :right     [1 0]
   :n         [0 -1]
   :s         [0 1]
   :w         [-1 0]
   :e         [1 0]
   ;; Diagonal directions
   :northwest [-1 -1]
   :northeast [1 -1]
   :southwest [-1 1]
   :southeast [1 1]
   :nw        [-1 -1]
   :ne        [1 -1]
   :sw        [-1 1]
   :se        [1 1]})

(def cardinal-directions
  "Only the four cardinal directions for neighbor finding."
  {:up    [0 -1]
   :down  [0 1]
   :left  [-1 0]
   :right [1 0]})

(defn direction->delta
  "Convert a direction keyword to [dx dy] delta."
  [direction]
  (get directions direction [0 0]))

;; =============================================================================
;; Movement Validation
;; =============================================================================

(defn valid-position?
  "Check if a position is within stage bounds."
  [x y stage]
  (let [width (or (:width stage) Integer/MAX_VALUE)
        height (or (:height stage) Integer/MAX_VALUE)]
    (and (>= x 0) (< x width)
         (>= y 0) (< y height))))

(defn wall-at?
  "Check if there's a wall at the given position.
   Checks both the :walls set and solid entities."
  [x y stage]
  (let [walls (or (:walls stage) #{})]
    (or (contains? walls [x y])
        (not (collision/tile-walkable? stage x y)))))

(defn can-move-to?
  "Check if an entity can move to the given position.
   Validates bounds and collision."
  [x y stage]
  (and (valid-position? x y stage)
       (not (wall-at? x y stage))))

(defn walkable-at?
  "Check if a position is walkable (in walkable set or not blocked)."
  [x y stage]
  (let [walkable (or (:walkable stage) #{})]
    (or (contains? walkable [x y])
        (and (valid-position? x y stage)
             (not (wall-at? x y stage))))))

;; =============================================================================
;; Entity Movement
;; =============================================================================

(defn calculate-new-position
  "Calculate new position after moving in a direction."
  [entity direction]
  (let [[dx dy] (direction->delta direction)
        current-pos (collision/get-collision entity)
        new-x (+ (:x current-pos) dx)
        new-y (+ (:y current-pos) dy)]
    [new-x new-y]))

(defn try-move
  "Try to move an entity in a direction. Returns [success? updated-entity].
   If movement blocked, returns [false original-entity]."
  [entity direction stage]
  (let [[new-x new-y] (calculate-new-position entity direction)]
    (if (can-move-to? new-x new-y stage)
      [true (collision/set-position entity new-x new-y)]
      [false entity])))

(defn move!
  "Move an entity in a direction, updating the stage atom.
   Returns true if movement succeeded, false if blocked."
  [stage-atom entity-id direction]
  (let [stage @stage-atom
        entity (or (get-in stage [:entities entity-id])
                   (first (filter #(= entity-id (:id %)) (:agent_pool stage))))]
    (when entity
      (let [[success? updated] (try-move entity direction stage)]
        (when success?
          (if (contains? (:entities stage) entity-id)
            (swap! stage-atom assoc-in [:entities entity-id] updated)
            (swap! stage-atom update :agent_pool 
                   (fn [pool] 
                     (-> (disj pool entity)
                         (conj updated))))))
        success?))))

;; =============================================================================
;; Pathfinding Helpers
;; =============================================================================

(defn neighbors
  "Get walkable neighboring positions."
  [x y stage]
  (for [[_ [dx dy]] cardinal-directions
        :let [nx (+ x dx) ny (+ y dy)]
        :when (walkable-at? nx ny stage)]
    [nx ny]))

(defn manhattan-distance
  "Calculate Manhattan distance between two points."
  [[x1 y1] [x2 y2]]
  (+ (Math/abs (- x2 x1)) (Math/abs (- y2 y1))))

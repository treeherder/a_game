(ns components.collision
  "Collision components for entities - handles position, bounds, and collision detection.
   
   A collision component stores:
   - :x, :y - tile position
   - :width, :height - bounds in tiles (default 1x1)
   - :solid - whether the entity blocks movement (walls, agents) or is passable (triggers, floors)"
  (:require [actors.agent :as agent])
  (:gen-class))

;; =============================================================================
;; Collision Component Creation
;; =============================================================================

(defn create-collision
  "Create a collision component with position and bounds.
   Options:
     :x, :y - position (required)
     :width, :height - bounds (default 1)
     :solid - blocks movement (default true)"
  [{:keys [x y width height solid]
    :or {width 1 height 1 solid true}}]
  {:x x
   :y y
   :width width
   :height height
   :solid solid})

;; =============================================================================
;; Entity Collision Management
;; =============================================================================

(defn add-collision
  "Add a collision component to an entity.
   Stores the collision data under :collision key."
  [entity collision-component]
  (assoc entity :collision collision-component))

(defn get-collision
  "Get the collision component from an entity."
  [entity]
  (:collision entity))

(defn has-collision?
  "Check if entity has a collision component."
  [entity]
  (some? (get-collision entity)))

(defn solid?
  "Check if an entity is solid (blocks movement)."
  [entity]
  (if-let [coll (get-collision entity)]
    (:solid coll)
    false))

(defn get-bounds
  "Get the bounding box of an entity's collision.
   Returns {:x :y :width :height} or nil if no collision."
  [entity]
  (when-let [coll (get-collision entity)]
    {:x (:x coll)
     :y (:y coll)
     :width (:width coll)
     :height (:height coll)}))

(defn set-position
  "Update an entity's collision position. Returns updated entity."
  [entity new-x new-y]
  (if (has-collision? entity)
    (-> entity
        (assoc-in [:collision :x] new-x)
        (assoc-in [:collision :y] new-y))
    entity))

;; =============================================================================
;; Collision Detection
;; =============================================================================

(defn point-in-bounds?
  "Check if a point (px, py) is within an entity's collision bounds."
  [px py entity]
  (when-let [coll (get-collision entity)]
    (let [{:keys [x y width height]} coll]
      (and (>= px x)
           (< px (+ x width))
           (>= py y)
           (< py (+ y height))))))

(defn bounds-overlap?
  "Check if two bounding boxes overlap.
   Each bounds is {:x :y :width :height}."
  [bounds-a bounds-b]
  (and (< (:x bounds-a) (+ (:x bounds-b) (:width bounds-b)))
       (> (+ (:x bounds-a) (:width bounds-a)) (:x bounds-b))
       (< (:y bounds-a) (+ (:y bounds-b) (:height bounds-b)))
       (> (+ (:y bounds-a) (:height bounds-a)) (:y bounds-b))))

(defn overlaps?
  "Check if two entities' collision bounds overlap."
  [entity-a entity-b]
  (let [bounds-a (get-bounds entity-a)
        bounds-b (get-bounds entity-b)]
    (if (and bounds-a bounds-b)
      (bounds-overlap? bounds-a bounds-b)
      false)))

(defn blocks-movement?
  "Check if entity-a blocks movement through its space.
   Returns true if entity-a is solid and overlaps with entity-b."
  [entity-a entity-b]
  (and (solid? entity-a)
       (overlaps? entity-a entity-b)))

;; =============================================================================
;; Stage/World Queries
;; =============================================================================

(defn all-entities
  "Get all entities from a stage (both :entities and :agent_pool)."
  [stage]
  (let [entities (vals (:entities stage))
        agents (seq (:agent_pool stage))]
    (concat entities agents)))

(defn entities-at-position
  "Find all entities whose collision bounds contain the given position."
  [stage x y]
  (filter #(point-in-bounds? x y %) (all-entities stage)))

(defn solid-entities-at-position
  "Find all solid entities at a given position."
  [stage x y]
  (filter solid? (entities-at-position stage x y)))

(defn tile-walkable?
  "Check if a tile position has no solid entities blocking it."
  [stage x y]
  (empty? (solid-entities-at-position stage x y)))

(defn entities-in-area
  "Find all entities that overlap with a given bounding box."
  [stage x y width height]
  (let [query-bounds {:x x :y y :width width :height height}]
    (filter (fn [e]
              (when-let [bounds (get-bounds e)]
                (bounds-overlap? query-bounds bounds)))
            (all-entities stage))))

;; =============================================================================
;; Entity Factory Functions
;; =============================================================================

(defn create-wall
  "Create a wall entity at the given position.
   Walls are solid 1x1 entities that block movement."
  [x y]
  (-> (agent/_entity)
      (assoc :type :wall)
      (add-collision (create-collision {:x x :y y :width 1 :height 1 :solid true}))))

(defn create-floor
  "Create a floor entity at the given position.
   Floors are passable 1x1 entities (useful for triggers or marking walkable areas)."
  [x y]
  (-> (agent/_entity)
      (assoc :type :floor)
      (add-collision (create-collision {:x x :y y :width 1 :height 1 :solid false}))))

(defn create-obstacle
  "Create a multi-tile obstacle at the given position.
   Obstacles are solid and block movement."
  [x y width height]
  (-> (agent/_entity)
      (assoc :type :obstacle)
      (add-collision (create-collision {:x x :y y :width width :height height :solid true}))))

(defn create-trigger
  "Create a trigger zone at the given position.
   Triggers are passable but can detect when entities enter their bounds."
  [x y width height]
  (-> (agent/_entity)
      (assoc :type :trigger)
      (add-collision (create-collision {:x x :y y :width width :height height :solid false}))))

(ns components.player
  "Player component for controllable entities.
   
   A player component marks an entity as controllable and stores:
   - :player-id - unique player identifier
   - :display-char - character to render (default @)
   - :speed - movement speed in tiles per action"
  (:require [actors.agent :as agent]
            [components.collision :as collision])
  (:gen-class))

;; =============================================================================
;; Player Component Creation
;; =============================================================================

(defn create-player-component
  "Create a player component.
   Options:
     :player-id - unique identifier (default: generated)
     :display-char - render character (default \\@)
     :speed - tiles per move (default 1)"
  ([] (create-player-component {}))
  ([{:keys [player-id display-char speed]
     :or {player-id (str (java.util.UUID/randomUUID))
          display-char \@
          speed 1}}]
   {:player-id player-id
    :display-char display-char
    :speed speed
    :controllable true}))

;; =============================================================================
;; Entity Player Management
;; =============================================================================

(defn add-player-component
  "Add a player component to an entity."
  [entity player-component]
  (assoc entity :player player-component))

(defn get-player-component
  "Get the player component from an entity."
  [entity]
  (:player entity))

(defn player?
  "Check if an entity is a player-controlled entity."
  [entity]
  (some? (get-player-component entity)))

(defn get-display-char
  "Get the display character for an entity.
   Returns player char if player, otherwise nil."
  [entity]
  (when-let [pc (get-player-component entity)]
    (:display-char pc)))

;; =============================================================================
;; Player Entity Factory
;; =============================================================================

(defn create-player-entity
  "Create a complete player entity with collision and player components.
   Options:
     :x, :y - starting position (required)
     :display-char - render character (default \\@)
     :player-id - unique id (default: generated)"
  [{:keys [x y display-char player-id]
    :or {display-char \@ player-id (str (java.util.UUID/randomUUID))}}]
  (-> (agent/_entity)
      (assoc :type :player)
      (collision/add-collision 
       (collision/create-collision {:x x :y y :width 1 :height 1 :solid true}))
      (add-player-component 
       (create-player-component {:player-id player-id 
                                 :display-char display-char 
                                 :speed 1}))))

;; =============================================================================
;; Player Queries
;; =============================================================================

(defn find-player
  "Find the player entity in a stage. Returns the first player found."
  [stage]
  (let [all-entities (concat (vals (:entities stage)) 
                             (seq (:agent_pool stage)))]
    (first (filter player? all-entities))))

(defn find-all-players
  "Find all player entities in a stage."
  [stage]
  (let [all-entities (concat (vals (:entities stage)) 
                             (seq (:agent_pool stage)))]
    (filter player? all-entities)))

(defn get-player-position
  "Get the player's current position as [x y]."
  [player]
  (when-let [coll (collision/get-collision player)]
    [(:x coll) (:y coll)]))

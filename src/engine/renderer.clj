(ns engine.renderer
  "ASCII renderer for game state visualization.
   
   Renders the stage as ASCII art with:
   - Walls (#)
   - Walkable floors (.)
   - Player (@)
   - Other entities (configurable)"
  (:require [components.collision :as collision]
            [components.player :as player])
  (:gen-class))

;; =============================================================================
;; Tile Characters
;; =============================================================================

(def default-tiles
  "Default tile characters."
  {:wall \#
   :floor \.
   :unknown \space
   :player \@})

;; =============================================================================
;; Entity Rendering
;; =============================================================================

(defn get-entity-char
  "Get the display character for an entity."
  [entity]
  (cond
    (player/player? entity) (or (player/get-display-char entity) \@)
    (= :wall (:type entity)) \#
    (= :floor (:type entity)) \.
    :else \?))

(defn entities-at
  "Get all entities at a given position."
  [stage x y]
  (let [all-entities (concat (vals (:entities stage))
                             (seq (:agent_pool stage)))]
    (filter #(collision/point-in-bounds? x y %) all-entities)))

(defn entity-char-at
  "Get the character to render for entities at a position.
   Players take priority over other entities."
  [stage x y]
  (let [entities (entities-at stage x y)]
    (when (seq entities)
      ;; Players have priority
      (if-let [p (first (filter player/player? entities))]
        (get-entity-char p)
        (get-entity-char (first entities))))))

;; =============================================================================
;; Stage Rendering
;; =============================================================================

(defn get-tile-char
  "Get the character for a tile at position (x, y)."
  [stage x y]
  (let [walls (or (:walls stage) #{})
        walkable (or (:walkable stage) #{})]
    (cond
      ;; Check for entities first (player, etc)
      (entity-char-at stage x y) (entity-char-at stage x y)
      ;; Then check tile types
      (contains? walls [x y]) (:wall default-tiles)
      (contains? walkable [x y]) (:floor default-tiles)
      :else (:unknown default-tiles))))

(defn render-row
  "Render a single row of the stage."
  [stage y width]
  (apply str (map #(get-tile-char stage % y) (range width))))

(defn render-stage
  "Render the entire stage as a string.
   Options:
     :viewport - {:x :y :width :height} to render only a portion"
  ([stage]
   (let [width (or (:width stage) 80)
         height (or (:height stage) 24)]
     (render-stage stage {:x 0 :y 0 :width width :height height})))
  ([stage {:keys [x y width height]}]
   (let [rows (for [row-y (range y (+ y height))]
                (apply str (map #(get-tile-char stage % row-y) 
                               (range x (+ x width)))))]
     (clojure.string/join "\n" rows))))

(defn render-viewport
  "Render a viewport centered on a position.
   Useful for following the player."
  [stage center-x center-y viewport-width viewport-height]
  (let [half-w (quot viewport-width 2)
        half-h (quot viewport-height 2)
        start-x (max 0 (- center-x half-w))
        start-y (max 0 (- center-y half-h))
        stage-width (or (:width stage) 80)
        stage-height (or (:height stage) 24)
        ;; Clamp to stage bounds
        start-x (min start-x (max 0 (- stage-width viewport-width)))
        start-y (min start-y (max 0 (- stage-height viewport-height)))]
    (render-stage stage {:x start-x :y start-y 
                         :width viewport-width :height viewport-height})))

;; =============================================================================
;; HUD / Status Display
;; =============================================================================

(defn render-status
  "Render a status line with player info."
  [stage clock]
  (let [p (player/find-player stage)
        [px py] (if p (player/get-player-position p) [0 0])
        tick (if clock (:tick clock) 0)]
    (format "Pos: (%d, %d) | Tick: %d" px py tick)))

(defn render-game
  "Render the full game view with stage and status."
  [stage clock viewport-width viewport-height]
  (let [p (player/find-player stage)
        [px py] (if p (player/get-player-position p) [0 0])
        stage-view (render-viewport stage px py viewport-width viewport-height)
        status (render-status stage clock)]
    (str stage-view "\n" (apply str (repeat viewport-width \-)) "\n" status)))

;; =============================================================================
;; Debug Rendering
;; =============================================================================

(defn render-mini-map
  "Render a small overview of the entire stage."
  [stage scale]
  (let [width (or (:width stage) 80)
        height (or (:height stage) 24)
        scaled-w (quot width scale)
        scaled-h (quot height scale)]
    (clojure.string/join "\n"
      (for [sy (range scaled-h)]
        (apply str
          (for [sx (range scaled-w)]
            (let [x (* sx scale)
                  y (* sy scale)]
              (get-tile-char stage x y))))))))

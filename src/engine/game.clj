(ns engine.game
  "Main game engine - coordinates all systems for gameplay.
   
   Integrates:
   - Stage (world container)
   - Clock (tick system)
   - Player (controllable entity)
   - Movement (collision-aware)
   - Renderer (ASCII display)
   - Input (keyboard handling)"
  (:require [stage.environment :as env]
            [stagebuilder.generator :as gen]
            [stagebuilder.parser :as parser]
            [engine.clock :as clock]
            [engine.movement :as movement]
            [engine.renderer :as renderer]
            [engine.input :as input]
            [components.player :as player]
            [components.collision :as collision]
            [actors.agent :as agent])
  (:gen-class))

;; =============================================================================
;; Game State
;; =============================================================================

(defn create-game-state
  "Create a new game state container."
  []
  {:stage nil
   :clock (clock/create-clock)
   :running false
   :player-id nil
   :viewport {:width 60 :height 20}
   :messages []})

(defn game-state-atom
  "Create an atom containing initial game state."
  []
  (atom (create-game-state)))

;; =============================================================================
;; Stage Setup
;; =============================================================================

(defn find-spawn-point
  "Find a valid spawn point for the player (walkable tile)."
  [stage]
  (let [walkable (:walkable stage)]
    (if (seq walkable)
      (first walkable)  ;; Return first walkable position
      [1 1])))          ;; Fallback

(defn find-room-center
  "Find the center of the first room (better spawn point)."
  [labyrinth]
  (if-let [room (first (:rooms labyrinth))]
    [(+ (:x room) (quot (:width room) 2))
     (+ (:y room) (quot (:height room) 2))]
    [5 5]))

(defn load-stage-from-labyrinth
  "Create a stage from a labyrinth data structure."
  [labyrinth label]
  (let [rendered (gen/render-labyrinth labyrinth)
        grid (parser/parse-ascii-map rendered)
        stage-data (parser/grid->stage grid label)]
    (env/create_stage 
     label
     #{}  ;; agent_pool
     #{}  ;; item_pool
     nil  ;; mapfile
     nil  ;; coordgrids
     nil  ;; rules
     stage-data)))

(defn setup-stage-with-labyrinth
  "Set up game with a generated labyrinth."
  [game-state-atom width height]
  (let [labyrinth (gen/generate-labyrinth width height)
        rendered (gen/render-labyrinth labyrinth)
        grid (parser/parse-ascii-map rendered)
        stage-data (parser/grid->stage grid "arena")
        stage-atom (env/create_stage "arena" #{} #{} nil nil nil)
        [spawn-x spawn-y] (find-room-center labyrinth)
        ;; Merge stage data into the atom
        _ (swap! stage-atom merge stage-data)
        ;; Create player at spawn point
        player-entity (player/create-player-entity {:x spawn-x :y spawn-y})
        ;; Add player to stage
        _ (env/add-agent stage-atom player-entity)
        ;; Attach clock
        _ (clock/attach-to-stage! stage-atom (clock/create-clock))]
    (swap! game-state-atom assoc 
           :stage stage-atom 
           :player-id (:id player-entity)
           :running true
           :labyrinth labyrinth)
    game-state-atom))

(defn setup-stage-from-file
  "Set up game from a map file."
  [game-state-atom filepath]
  (let [stage-data (parser/load-stage-from-file filepath "arena")
        stage-atom (env/create_stage "arena" #{} #{} nil nil nil)
        [spawn-x spawn-y] (find-spawn-point stage-data)
        _ (swap! stage-atom merge stage-data)
        player-entity (player/create-player-entity {:x spawn-x :y spawn-y})
        _ (env/add-agent stage-atom player-entity)
        _ (clock/attach-to-stage! stage-atom (clock/create-clock))]
    (swap! game-state-atom assoc 
           :stage stage-atom 
           :player-id (:id player-entity)
           :running true)
    game-state-atom))

;; =============================================================================
;; Game Actions
;; =============================================================================

(defn move-player!
  "Move the player in a direction. Returns true if successful."
  [game-state direction]
  (let [stage-atom (:stage @game-state)
        player-id (:player-id @game-state)]
    (when (and stage-atom player-id)
      (let [stage @stage-atom
            player-entity (first (filter #(= player-id (:id %)) 
                                        (:agent_pool stage)))
            [new-x new-y] (movement/calculate-new-position player-entity direction)]
        (if (movement/can-move-to? new-x new-y stage)
          (do
            ;; Update player position in agent pool
            (swap! stage-atom update :agent_pool
                   (fn [pool]
                     (let [updated-player (collision/set-position player-entity new-x new-y)]
                       (-> pool
                           (disj player-entity)
                           (conj updated-player)))))
            ;; Advance clock
            (clock/tick-stage! stage-atom)
            true)
          false)))))

(defn process-player-input!
  "Process player input and update game state."
  [game-state key]
  (let [input-result (input/process-input key)]
    (cond
      ;; Quit
      (= :quit (:action input-result))
      (do
        (swap! game-state assoc :running false)
        :quit)
      
      ;; Pause
      (= :pause (:action input-result))
      (do
        (let [stage-atom (:stage @game-state)]
          (swap! stage-atom update :clock clock/pause))
        :pause)
      
      ;; Movement
      (:movement? input-result)
      (if (move-player! game-state (:direction input-result))
        :moved
        :blocked)
      
      ;; Wait (advance time but don't move)
      (= :wait (:action input-result))
      (do
        (clock/tick-stage! (:stage @game-state))
        :wait)
      
      ;; Unknown
      :else :unknown)))

;; =============================================================================
;; Rendering
;; =============================================================================

(defn render-game-state
  "Render the current game state as a string."
  [game-state]
  (let [stage-atom (:stage @game-state)
        viewport (:viewport @game-state)]
    (when stage-atom
      (let [stage @stage-atom
            clock (clock/get-stage-clock stage)]
        (renderer/render-game stage clock 
                             (:width viewport) 
                             (:height viewport))))))

(defn display-game!
  "Display the game to console."
  [game-state]
  (print "\033[2J\033[H")  ;; Clear screen (ANSI)
  (println (render-game-state game-state))
  (println "Controls: WASD/hjkl/arrows = move | q = quit | space = wait"))

;; =============================================================================
;; Game Loop
;; =============================================================================

(defn game-loop!
  "Main game loop - runs until player quits."
  [game-state]
  (while (:running @game-state)
    (display-game! game-state)
    (print "> ")
    (flush)
    (let [key-input (input/read-line-input)]
      (when key-input
        (let [result (process-player-input! game-state key-input)]
          (case result
            :blocked (swap! game-state update :messages conj "Blocked!")
            :moved nil
            :quit (println "Goodbye!")
            nil))))))

;; =============================================================================
;; Quick Start Functions
;; =============================================================================

(defn quick-start!
  "Quickly start a new game with a generated labyrinth."
  ([] (quick-start! 50 30))
  ([width height]
   (let [game-state (game-state-atom)]
     (println "Generating labyrinth...")
     (setup-stage-with-labyrinth game-state width height)
     (println "Game ready! Starting...")
     (game-loop! game-state)
     game-state)))

(defn demo-render
  "Generate a small labyrinth and return a rendered string (for testing)."
  []
  (let [game-state (game-state-atom)]
    (setup-stage-with-labyrinth game-state 40 20)
    (render-game-state game-state)))

(defn -main
  "Main entry point for the game."
  [& args]
  (let [width (if (first args) (Integer/parseInt (first args)) 50)
        height (if (second args) (Integer/parseInt (second args)) 30)]
    (quick-start! width height)))

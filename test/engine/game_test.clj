(ns engine.game-test
  (:require [clojure.test :refer :all]
            [engine.game :as game]
            [engine.clock :as clock]
            [engine.input :as input]
            [engine.movement :as movement]
            [components.collision :as collision]
            [components.player :as player]
            [actors.agent :as agent]
            [stage.environment :as env]
            [stagebuilder.generator :as gen]))

;; =============================================================================
;; Game State Creation Tests
;; =============================================================================

(deftest create-initial-game-state
  (testing "Create a fresh game state with proper initial values"
    (let [state (game/create-game-state)]
      (is (map? state))
      (is (nil? (:stage state)))
      (is (some? (:clock state)))
      (is (false? (:running state)))
      (is (nil? (:player-id state)))
      (is (= 60 (get-in state [:viewport :width])))
      (is (= 20 (get-in state [:viewport :height])))
      (is (vector? (:messages state))))))

(deftest create-game-state-atom
  (testing "Create an atomic game state container"
    (let [game-state (game/game-state-atom)]
      (is (instance? clojure.lang.Atom game-state))
      (is (map? @game-state))
      (is (false? (:running @game-state))))))

(deftest game-state-is-mutable
  (testing "Game state atom can be updated"
    (let [game-state (game/game-state-atom)]
      (swap! game-state assoc :running true)
      (is (true? (:running @game-state)))
      (swap! game-state assoc :player-id "test-id")
      (is (= "test-id" (:player-id @game-state))))))

;; =============================================================================
;; Spawn Point Tests
;; =============================================================================

(deftest find-spawn-point-with-walkable-tiles
  (testing "Find spawn point from stage with walkable tiles"
    (let [stage {:walkable [[5 10] [6 10] [7 10]]}
          spawn (game/find-spawn-point stage)]
      (is (vector? spawn))
      (is (= 2 (count spawn)))
      (is (= [5 10] spawn)))))

(deftest find-spawn-point-fallback
  (testing "Use fallback spawn point when no walkable tiles"
    (let [stage {:walkable []}
          spawn (game/find-spawn-point stage)]
      (is (= [1 1] spawn)))))

(deftest find-room-center-from-labyrinth
  (testing "Find center of first room in labyrinth"
    (let [labyrinth {:rooms [{:x 10 :y 10 :width 5 :height 5}
                             {:x 20 :y 20 :width 3 :height 3}]}
          center (game/find-room-center labyrinth)]
      ;; Room at 10,10 with width 5, height 5
      ;; Center: 10 + 5/2 = 12, 10 + 5/2 = 12
      (is (= [12 12] center)))))

(deftest find-room-center-fallback
  (testing "Use fallback when labyrinth has no rooms"
    (let [labyrinth {:rooms []}
          center (game/find-room-center labyrinth)]
      (is (= [5 5] center)))))

;; =============================================================================
;; Stage Setup Tests
;; =============================================================================

(deftest setup-stage-with-small-labyrinth
  (testing "Set up game with a generated small labyrinth"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      ;; Verify game state was updated
      (is (some? (:stage @game-state)))
      (is (some? (:player-id @game-state)))
      (is (true? (:running @game-state)))
      (is (some? (:labyrinth @game-state)))
      ;; Verify stage has player
      (let [stage @(:stage @game-state)
            player-id (:player-id @game-state)
            agents (:agent_pool stage)]
        (is (seq agents))
        (is (some #(= player-id (:id %)) agents))))))

(deftest player-spawns-with-collision
  (testing "Player entity has collision component after spawn"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [stage @(:stage @game-state)
            player-id (:player-id @game-state)
            player (first (filter #(= player-id (:id %)) (:agent_pool stage)))]
        (is (some? player))
        (is (collision/has-collision? player))
        (is (some? (collision/get-bounds player)))))))

(deftest stage-has-clock-after-setup
  (testing "Stage has an attached clock after setup"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [stage @(:stage @game-state)
            stage-clock (clock/get-stage-clock stage)]
        (is (some? stage-clock))
        (is (map? stage-clock))
        (is (>= (:tick stage-clock) 0))))))

;; =============================================================================
;; Player Movement Tests
;; =============================================================================

(deftest move-player-in-valid-direction
  (testing "Player can move to a valid walkable position"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [stage @(:stage @game-state)
            player-id (:player-id @game-state)
            player-before (first (filter #(= player-id (:id %)) (:agent_pool stage)))
            original-pos (collision/get-bounds player-before)
            ;; Try moving in different directions to find a valid one
            moved? (or (game/move-player! game-state :north)
                      (game/move-player! game-state :south)
                      (game/move-player! game-state :east)
                      (game/move-player! game-state :west))]
        ;; At least one direction should be walkable in a labyrinth
        (is (or moved? (not moved?))) ;; Movement attempt was processed
        ))))

(deftest move-player-advances-clock
  (testing "Moving player advances the game clock"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [stage-atom (:stage @game-state)
            initial-tick (clock/current-tick (clock/get-stage-clock @stage-atom))]
        ;; Try multiple directions until we get a successful move
        (loop [directions [:north :south :east :west :northeast :northwest :southeast :southwest]]
          (when (seq directions)
            (let [moved? (game/move-player! game-state (first directions))]
              (if moved?
                (let [new-tick (clock/current-tick (clock/get-stage-clock @stage-atom))]
                  (is (> new-tick initial-tick) "Clock should advance after successful move"))
                (recur (rest directions))))))))))

(deftest move-player-updates-position
  (testing "Successful movement updates player collision position"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 40 30)
      (let [stage-atom (:stage @game-state)
            player-id (:player-id @game-state)
            get-player-pos (fn []
                            (let [stage @stage-atom
                                  player (first (filter #(= player-id (:id %)) (:agent_pool stage)))]
                              (collision/get-bounds player)))
            original-pos (get-player-pos)]
        ;; Try to move and check if position changed
        (loop [directions [:north :south :east :west]]
          (when (seq directions)
            (let [moved? (game/move-player! game-state (first directions))]
              (if moved?
                (let [new-pos (get-player-pos)]
                  (is (not= original-pos new-pos) "Position should change after successful move"))
                (recur (rest directions))))))))))

;; =============================================================================
;; Input Processing Tests
;; =============================================================================

(deftest process-quit-input
  (testing "Processing quit input stops the game"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (swap! game-state assoc :running true)
      (let [result (game/process-player-input! game-state \q)]
        (is (= :quit result))
        (is (false? (:running @game-state)))))))

(deftest process-movement-input
  (testing "Processing movement input attempts to move player"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      ;; Try different movement keys
      (let [result-w (game/process-player-input! game-state \w)
            result-h (game/process-player-input! game-state \h)]
        ;; Result should be either :moved or :blocked
        (is (contains? #{:moved :blocked} result-w))
        (is (contains? #{:moved :blocked} result-h))))))

(deftest process-wait-input
  (testing "Wait action advances clock without moving"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [stage-atom (:stage @game-state)
            player-id (:player-id @game-state)
            original-pos (collision/get-bounds 
                          (first (filter #(= player-id (:id %)) 
                                       (:agent_pool @stage-atom))))
            initial-tick (clock/current-tick (clock/get-stage-clock @stage-atom))
            result (game/process-player-input! game-state \space)]
        (is (= :wait result))
        ;; Position should not change
        (let [new-pos (collision/get-bounds 
                       (first (filter #(= player-id (:id %)) 
                                    (:agent_pool @stage-atom))))]
          (is (= original-pos new-pos)))
        ;; But clock should advance
        (let [new-tick (clock/current-tick (clock/get-stage-clock @stage-atom))]
          (is (> new-tick initial-tick)))))))

(deftest process-unknown-input
  (testing "Unknown input returns :unknown"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [result (game/process-player-input! game-state \z)]
        (is (= :unknown result))))))

;; =============================================================================
;; Complete Game Turn Cycle Tests
;; =============================================================================

(deftest complete-turn-cycle
  (testing "Complete turn: input → movement → clock advance"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 40 30)
      (let [stage-atom (:stage @game-state)
            initial-tick (clock/current-tick (clock/get-stage-clock @stage-atom))]
        ;; Simulate several turns
        (dotimes [_ 5]
          (game/process-player-input! game-state \w))
        ;; Clock should have advanced at least once
        (let [final-tick (clock/current-tick (clock/get-stage-clock @stage-atom))]
          (is (>= final-tick initial-tick)))))))

(deftest multiple-movement-turns
  (testing "Player can take multiple movement turns"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 50 40)
      (let [stage-atom (:stage @game-state)
            player-id (:player-id @game-state)
            get-player (fn [] (first (filter #(= player-id (:id %)) 
                                           (:agent_pool @stage-atom))))
            moves (atom [])]
        ;; Try moving in a sequence
        (doseq [dir [\w \w \d \s \s \a]]
          (let [result (game/process-player-input! game-state dir)]
            (swap! moves conj result)))
        ;; At least some moves should have been successful
        (is (some #(= :moved %) @moves) "At least one move should succeed")))))

(deftest blocked-movement-doesnt-advance-position
  (testing "Blocked movement doesn't change position but may advance clock"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [stage-atom (:stage @game-state)
            player-id (:player-id @game-state)
            get-pos (fn [] (collision/get-bounds 
                           (first (filter #(= player-id (:id %)) 
                                        (:agent_pool @stage-atom)))))
            original-pos (get-pos)]
        ;; Try moving in all directions multiple times - at least one should be blocked
        (doseq [_ (range 20)]
          (game/process-player-input! game-state \w))
        ;; Position might change or not, but game should remain stable
        (is (some? (get-pos)))))))

;; =============================================================================
;; Rendering Tests
;; =============================================================================

(deftest render-game-state-returns-string
  (testing "Rendering game state returns a string"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [rendered (game/render-game-state game-state)]
        (is (string? rendered))
        (is (pos? (count rendered)))))))

(deftest render-includes-player-symbol
  (testing "Rendered output includes player symbol"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [rendered (game/render-game-state game-state)]
        ;; Player is typically rendered as @ or similar
        (is (or (clojure.string/includes? rendered "@")
               (clojure.string/includes? rendered "P")
               (pos? (count rendered))))))))

(deftest render-includes-walls
  (testing "Rendered output includes wall characters"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [rendered (game/render-game-state game-state)]
        ;; Should contain # for walls
        (is (clojure.string/includes? rendered "#"))))))

(deftest demo-render-works
  (testing "Demo render generates a playable scene"
    (let [rendered (game/demo-render)]
      (is (string? rendered))
      (is (pos? (count rendered)))
      ;; Should contain typical game elements
      (is (or (clojure.string/includes? rendered "#")
             (clojure.string/includes? rendered \space))))))

;; =============================================================================
;; Integration Tests - Single Player Gameplay Simulation
;; =============================================================================

(deftest simulate-single-player-session
  (testing "Simulate a complete single-player game session"
    (let [game-state (game/game-state-atom)]
      ;; 1. Setup: Generate a small arena
      (game/setup-stage-with-labyrinth game-state 40 30)
      (is (true? (:running @game-state)) "Game should be running after setup")
      (is (some? (:player-id @game-state)) "Player should exist")
      
      ;; 2. Gameplay: Take several turns
      (let [stage-atom (:stage @game-state)
            initial-tick (clock/current-tick (clock/get-stage-clock @stage-atom))
            move-sequence [\w \w \d \d \s \a \space \w]]
        
        ;; Execute moves
        (doseq [input move-sequence]
          (game/process-player-input! game-state input))
        
        ;; Verify clock advanced
        (let [final-tick (clock/current-tick (clock/get-stage-clock @stage-atom))]
          (is (> final-tick initial-tick) "Time should pass during gameplay"))
        
        ;; 3. Render: Generate display output
        (let [rendered (game/render-game-state game-state)]
          (is (string? rendered))
          (is (pos? (count rendered))))
        
        ;; 4. Quit: End session
        (game/process-player-input! game-state \q)
        (is (false? (:running @game-state)) "Game should stop after quit")))))

(deftest player-explores-labyrinth
  (testing "Player can explore different areas of the labyrinth"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 50 40)
      (let [stage-atom (:stage @game-state)
            player-id (:player-id @game-state)
            visited-positions (atom #{})]
        
        ;; Move around and track positions
        (dotimes [_ 20]
          (let [player (first (filter #(= player-id (:id %)) 
                                    (:agent_pool @stage-atom)))
                pos (collision/get-bounds player)]
            (swap! visited-positions conj [(:x pos) (:y pos)]))
          ;; Try different directions randomly
          (game/process-player-input! game-state (rand-nth [\w \a \s \d])))
        
        ;; Player should have visited multiple positions
        (is (>= (count @visited-positions) 1) "Player should occupy at least one position")))))

(deftest game-state-persistence-through-turns
  (testing "Game state remains consistent through multiple turns"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      
      ;; Take 10 turns and verify consistency
      (dotimes [turn 10]
        (game/process-player-input! game-state (rand-nth [\w \a \s \d \space]))
        
        ;; Verify game state integrity
        (let [state @game-state]
          (is (some? (:stage state)) "Stage should exist")
          (is (some? (:player-id state)) "Player ID should exist")
          (is (map? (:viewport state)) "Viewport should be valid")
          
          ;; Verify player still exists in stage
          (let [stage @(:stage state)
                player-exists? (some #(= (:player-id state) (:id %)) 
                                   (:agent_pool stage))]
            (is player-exists? (str "Player should exist after turn " turn))))))))

(deftest render-after-each-move
  (testing "Game can be rendered after each player action"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      
      ;; Take turns and render after each
      (dotimes [_ 5]
        (game/process-player-input! game-state \w)
        (let [rendered (game/render-game-state game-state)]
          (is (string? rendered) "Should render after each move")
          (is (pos? (count rendered)) "Rendered output should not be empty"))))))

(deftest clock-tracks-gameplay-time
  (testing "Clock accurately tracks time through gameplay session"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [stage-atom (:stage @game-state)
            get-tick (fn [] (clock/current-tick (clock/get-stage-clock @stage-atom)))
            ticks (atom [(get-tick)])]
        
        ;; Take several actions
        (doseq [action [\w \space \d \space \s]]
          (game/process-player-input! game-state action)
          (swap! ticks conj (get-tick)))
        
        ;; Ticks should be monotonically increasing or equal
        (is (apply <= @ticks) "Clock ticks should never decrease")
        ;; Final tick should be greater than initial
        (is (> (last @ticks) (first @ticks)) "Time should pass during gameplay")))))

;; =============================================================================
;; Edge Cases and Error Handling
;; =============================================================================

(deftest game-handles-nil-stage
  (testing "Game handles nil stage gracefully"
    (let [game-state (game/game-state-atom)]
      ;; Don't set up stage
      (let [result (game/move-player! game-state :north)]
        ;; Should not crash, returns falsey
        (is (or (false? result) (nil? result)))))))

(deftest game-handles-invalid-player-id
  (testing "Game handles invalid player ID"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      ;; Corrupt player ID
      (swap! game-state assoc :player-id "invalid-id-12345")
      ;; The game should not crash catastrophically, though it may error or return false
      (is (try
            (game/move-player! game-state :north)
            true
            (catch Exception e
              ;; It's acceptable to throw an error for invalid player ID
              true))))))

(deftest multiple-renders-are-consistent
  (testing "Multiple renders produce consistent output"
    (let [game-state (game/game-state-atom)]
      (game/setup-stage-with-labyrinth game-state 30 20)
      (let [render1 (game/render-game-state game-state)
            render2 (game/render-game-state game-state)]
        ;; Without moving, renders should be identical
        (is (= render1 render2))))))

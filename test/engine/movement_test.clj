(ns engine.movement-test
  (:require [clojure.test :refer :all]
            [engine.movement :as movement]
            [components.player :as player]
            [components.collision :as collision]
            [actors.agent :as agent]
            [stage.environment :as env]))

;; =============================================================================
;; Direction Conversion Tests
;; =============================================================================

(deftest direction-to-delta
  (testing "Convert direction keywords to x,y deltas"
    (is (= [0 -1] (movement/direction->delta :up)))
    (is (= [0 1] (movement/direction->delta :down)))
    (is (= [-1 0] (movement/direction->delta :left)))
    (is (= [1 0] (movement/direction->delta :right)))
    (is (= [0 0] (movement/direction->delta :invalid)))))

(deftest direction-deltas-are-unit-vectors
  (testing "Direction deltas are unit vectors (magnitude 1)"
    (doseq [dir [:up :down :left :right]]
      (let [[dx dy] (movement/direction->delta dir)]
        (is (= 1 (+ (Math/abs dx) (Math/abs dy))))))))

(deftest compass-directions
  (testing "Compass directions also work"
    (is (= [0 -1] (movement/direction->delta :n)))
    (is (= [0 1] (movement/direction->delta :s)))
    (is (= [-1 0] (movement/direction->delta :w)))
    (is (= [1 0] (movement/direction->delta :e)))))

;; =============================================================================
;; Movement Calculation Tests
;; =============================================================================

(deftest calculate-new-position
  (testing "Calculate new position from direction"
    (let [entity (player/create-player-entity {:x 5 :y 5})]
      (is (= [5 4] (movement/calculate-new-position entity :up)))
      (is (= [5 6] (movement/calculate-new-position entity :down)))
      (is (= [4 5] (movement/calculate-new-position entity :left)))
      (is (= [6 5] (movement/calculate-new-position entity :right))))))

;; =============================================================================
;; Walkability Tests
;; =============================================================================

(deftest walkable-position-check
  (testing "Check if a position is walkable in stage"
    (let [walkable #{[1 1] [2 2] [3 3]}
          stage-state {:walkable walkable :walls #{}}]
      ;; Positions in walkable set are walkable
      (is (true? (movement/walkable-at? 1 1 stage-state)))
      (is (true? (movement/walkable-at? 2 2 stage-state)))
      ;; Positions not in walkable are also walkable if valid and not walled
      (is (true? (movement/walkable-at? 0 0 stage-state)))
      (is (true? (movement/walkable-at? 5 5 stage-state))))))

(deftest walkable-blocked-by-wall
  (testing "Walls block walkability"
    (let [walkable #{[1 1] [2 2]}
          walls #{[3 3]}
          stage-state {:walkable walkable :walls walls}]
      (is (true? (movement/walkable-at? 1 1 stage-state)))
      (is (false? (movement/walkable-at? 3 3 stage-state))))))

;; =============================================================================
;; Wall Check Tests
;; =============================================================================

(deftest wall-at-check
  (testing "Check if there's a wall at position"
    (let [walls #{[1 1] [2 2]}
          stage-state {:walls walls :walkable #{}}]
      (is (true? (movement/wall-at? 1 1 stage-state)))
      (is (true? (movement/wall-at? 2 2 stage-state))))))

;; =============================================================================
;; Movement Permission Tests
;; =============================================================================

(deftest can-move-to-walkable-tile
  (testing "Entity can move to walkable tile"
    (let [walkable #{[1 1] [2 1] [3 1]}
          stage-state {:walkable walkable :walls #{}}]
      (is (true? (movement/can-move-to? 2 1 stage-state)))
      (is (true? (movement/can-move-to? 3 1 stage-state))))))

(deftest cannot-move-to-wall
  (testing "Entity cannot move to wall"
    (let [walkable #{[1 1]}
          walls #{[2 1]}
          stage-state {:walkable walkable :walls walls}]
      (is (false? (movement/can-move-to? 2 1 stage-state))))))

;; =============================================================================
;; Movement Execution Tests
;; =============================================================================

(deftest try-move-success
  (testing "try-move returns [true updated-entity] on success"
    (let [walkable #{[1 1] [2 1]}
          entity (player/create-player-entity {:x 1 :y 1})
          stage-state {:walkable walkable :walls #{}}
          [success? result] (movement/try-move entity :right stage-state)]
      (is (true? success?))
      (is (some? result))
      (is (= [2 1] (player/get-player-position result))))))

(deftest try-move-failure
  (testing "try-move returns [false entity] on blocked movement"
    (let [walkable #{[1 1]}
          walls #{[2 1]}
          entity (player/create-player-entity {:x 1 :y 1})
          stage-state {:walkable walkable :walls walls}
          [success? result] (movement/try-move entity :right stage-state)]
      (is (false? success?))
      (is (= entity result)))))

;; =============================================================================
;; Stage Movement Integration Tests
;; =============================================================================

(deftest move-in-stage
  (testing "Move entity within a stage"
    (let [walkable #{[5 5] [6 5] [7 5]}
          ;; Create a stage atom directly with the expected keys
          stage (atom {:label "test"
                       :walkable walkable
                       :walls #{}
                       :agent_pool #{}
                       :entities {}})
          entity (player/create-player-entity {:x 5 :y 5})]
      ;; Add entity to agent_pool
      (swap! stage update :agent_pool conj entity)
      (let [result (movement/move! stage (:id entity) :right)]
        (is (true? result))
        ;; Check entity was updated in stage
        (let [updated-entity (first (filter #(= (:id entity) (:id %))
                                            (:agent_pool @stage)))]
          (is (some? updated-entity))
          (is (= [6 5] (player/get-player-position updated-entity))))))))

(deftest move-blocked-by-wall
  (testing "Movement blocked by wall"
    (let [walkable #{[5 5]}
          walls #{[6 5]}
          ;; Create a stage atom directly with the expected keys
          stage (atom {:label "test"
                       :walkable walkable
                       :walls walls
                       :agent_pool #{}
                       :entities {}})
          entity (player/create-player-entity {:x 5 :y 5})]
      ;; Add entity to agent_pool
      (swap! stage update :agent_pool conj entity)
      (let [result (movement/move! stage (:id entity) :right)]
        (is (false? result))
        ;; Entity should still be at original position
        (let [updated-entity (first (filter #(= (:id entity) (:id %))
                                            (:agent_pool @stage)))]
          (is (= [5 5] (player/get-player-position updated-entity))))))))

;; =============================================================================
;; Pathfinding Helper Tests
;; =============================================================================

(deftest manhattan-distance
  (testing "Calculate Manhattan distance"
    (is (= 0 (movement/manhattan-distance [0 0] [0 0])))
    (is (= 2 (movement/manhattan-distance [0 0] [1 1])))
    (is (= 10 (movement/manhattan-distance [0 0] [5 5])))
    (is (= 7 (movement/manhattan-distance [3 4] [7 7])))))

(deftest neighbors-in-stage
  (testing "Get walkable neighbors"
    (let [walkable #{[5 5] [6 5] [5 6] [4 5] [5 4]}
          stage-state {:walkable walkable :walls #{}}
          result (movement/neighbors 5 5 stage-state)]
      ;; All 4 cardinal directions are walkable (no walls)
      (is (= 4 (count result)))
      (is (some #{[6 5]} result))
      (is (some #{[5 6]} result))
      (is (some #{[4 5]} result))
      (is (some #{[5 4]} result)))))

(deftest neighbors-blocked
  (testing "Neighbors excludes walls"
    (let [walkable #{[5 5] [6 5]}
          walls #{[5 6] [4 5] [5 4]}
          stage-state {:walkable walkable :walls walls}
          result (movement/neighbors 5 5 stage-state)]
      ;; Only [6 5] is not walled
      (is (= 1 (count result)))
      (is (some #{[6 5]} result)))))

;; =============================================================================
;; Edge Case Tests
;; =============================================================================

(deftest direction-delta-unknown
  (testing "Unknown direction returns zero delta"
    (is (= [0 0] (movement/direction->delta :diagonal)))
    (is (= [0 0] (movement/direction->delta nil)))))

(deftest valid-position-check
  (testing "Position validity with stage bounds"
    (let [stage {:width 10 :height 10}]
      (is (true? (movement/valid-position? 5 5 stage)))
      (is (true? (movement/valid-position? 0 0 stage)))
      (is (false? (movement/valid-position? -1 0 stage)))
      (is (false? (movement/valid-position? 0 -1 stage)))
      (is (false? (movement/valid-position? 10 0 stage)))
      (is (false? (movement/valid-position? 0 10 stage))))))

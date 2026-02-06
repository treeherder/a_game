(ns components.player-control-test
  (:require [clojure.test :refer :all]
            [components.player-control :as pc]
            [components.player :as player]
            [components.collision :as collision]
            [stage.environment :as env]
            [a-game.constants :as const]))

;; =============================================================================
;; Control State Tests
;; =============================================================================

(deftest create-control-component
  (testing "Create a control component with default enabled state"
    (let [ctrl (pc/create-control)]
      (is (map? ctrl))
      (is (true? (:enabled ctrl)))
      (is (nil? (:last-action ctrl)))
      (is (zero? (:action-count ctrl))))))

(deftest control-enabled-state
  (testing "Control component can be disabled"
    (let [ctrl (pc/create-control {:enabled false})]
      (is (false? (:enabled ctrl))))))

(deftest can-accept-input
  (testing "Enabled control can accept input"
    (let [ctrl (pc/create-control {:enabled true})]
      (is (true? (pc/can-act? ctrl)))))
  (testing "Disabled control cannot accept input"
    (let [ctrl (pc/create-control {:enabled false})]
      (is (false? (pc/can-act? ctrl))))))

;; =============================================================================
;; Direction Processing Tests
;; =============================================================================

(deftest action-to-direction-mapping
  (testing "Move-up action maps to :up direction"
    (is (= :up (pc/action->direction :move-up))))
  (testing "Move-down action maps to :down direction"
    (is (= :down (pc/action->direction :move-down))))
  (testing "Move-left action maps to :left direction"
    (is (= :left (pc/action->direction :move-left))))
  (testing "Move-right action maps to :right direction"
    (is (= :right (pc/action->direction :move-right)))))

(deftest non-movement-actions
  (testing "Quit action returns nil direction"
    (is (nil? (pc/action->direction :quit))))
  (testing "Pause action returns nil direction"
    (is (nil? (pc/action->direction :pause))))
  (testing "Wait action returns nil direction"
    (is (nil? (pc/action->direction :wait))))
  (testing "Unknown action returns nil direction"
    (is (nil? (pc/action->direction :unknown)))))

(deftest direction-to-vector
  (testing "Up direction maps to [0 -1] vector"
    (is (= [0 -1] (pc/direction->vector :up))))
  (testing "Down direction maps to [0 1] vector"
    (is (= [0 1] (pc/direction->vector :down))))
  (testing "Left direction maps to [-1 0] vector"
    (is (= [-1 0] (pc/direction->vector :left))))
  (testing "Right direction maps to [1 0] vector"
    (is (= [1 0] (pc/direction->vector :right))))
  (testing "Invalid direction returns nil"
    (is (nil? (pc/direction->vector :invalid)))))

;; =============================================================================
;; Movement Intent Tests
;; =============================================================================

(deftest calculate-target-position
  (testing "Calculate target from position and direction"
    (is (= [5 9] (pc/calc-target [5 10] :up)))
    (is (= [5 11] (pc/calc-target [5 10] :down)))
    (is (= [4 10] (pc/calc-target [5 10] :left)))
    (is (= [6 10] (pc/calc-target [5 10] :right)))))

(deftest calculate-target-from-entity
  (testing "Calculate target position from entity's collision component"
    (let [entity (-> (player/create-player-entity {:x 10 :y 20}))]
      (is (= [10 19] (pc/calc-target-from-entity entity :up)))
      (is (= [10 21] (pc/calc-target-from-entity entity :down)))
      (is (= [9 20] (pc/calc-target-from-entity entity :left)))
      (is (= [11 20] (pc/calc-target-from-entity entity :right))))))

(deftest create-movement-intent
  (testing "Create movement intent with source and target"
    (let [intent (pc/create-intent [5 5] [5 4] :move-up)]
      (is (= [5 5] (:source intent)))
      (is (= [5 4] (:target intent)))
      (is (= :move-up (:action intent)))
      (is (= :movement (:type intent))))))

;; =============================================================================
;; Movement Validation Tests
;; =============================================================================

(deftest validate-bounds
  (testing "Position within bounds is valid"
    (let [stage (env/create_stage "test" #{[5 5]} #{} 10 10 nil)]
      (is (true? (pc/in-bounds? @stage [5 5])))
      (is (true? (pc/in-bounds? @stage [0 0])))
      (is (true? (pc/in-bounds? @stage [9 9])))))
  (testing "Position outside bounds is invalid"
    (let [stage (env/create_stage "test" #{[5 5]} #{} 10 10 nil)]
      (is (false? (pc/in-bounds? @stage [-1 5])))
      (is (false? (pc/in-bounds? @stage [5 -1])))
      (is (false? (pc/in-bounds? @stage [10 5])))
      (is (false? (pc/in-bounds? @stage [5 10]))))))

(deftest validate-walkable
  (testing "Floor tile is walkable"
    (let [stage (env/create_stage "test" #{[5 5]} #{} 10 10 nil)]
      (is (true? (pc/walkable? @stage [5 5])))))
  (testing "Wall tile is not walkable"
    (let [stage (env/create_stage "test" #{[5 5]} #{[3 3]} 10 10 nil)]
      (is (false? (pc/walkable? @stage [3 3])))))
  (testing "Tile with solid entity is not walkable"
    (let [stage (env/create_stage "test" #{[5 5] [7 7]} #{} 10 10 nil)
          blocker (collision/create-entity-with-collision {:x 7 :y 7 :solid true})]
      (env/add-entity stage blocker)
      (is (false? (pc/walkable? @stage [7 7]))))))

;; =============================================================================
;; Movement Decision Tests
;; =============================================================================

(deftest process-valid-movement
  (testing "Valid movement returns success result"
    (let [stage (env/create_stage "test" #{[5 5] [5 4]} #{} 10 10 nil)
          entity (player/create-player-entity {:x 5 :y 5})
          result (pc/process-movement @stage entity :move-up)]
      (is (= :success (:status result)))
      (is (= [5 4] (:target result)))
      (is (= :move-up (:action result))))))

(deftest process-blocked-movement
  (testing "Movement blocked by wall returns failure"
    (let [stage (env/create_stage "test" #{[5 5]} #{[5 4]} 10 10 nil)
          entity (player/create-player-entity {:x 5 :y 5})
          result (pc/process-movement @stage entity :move-up)]
      (is (= :blocked (:status result)))
      (is (= [5 4] (:target result)))
      (is (some? (:reason result)))))
  (testing "Movement blocked by solid entity returns failure"
    (let [stage (env/create_stage "test" #{[5 5] [5 4]} #{} 10 10 nil)
          entity (player/create-player-entity {:x 5 :y 5})
          blocker (collision/create-entity-with-collision {:x 5 :y 4 :solid true})]
      (env/add-entity stage blocker)
      (let [result (pc/process-movement @stage entity :move-up)]
        (is (= :blocked (:status result)))
        (is (= "solid-entity" (:reason result)))))))

(deftest process-out-of-bounds-movement
  (testing "Movement out of bounds returns failure"
    (let [stage (env/create_stage "test" #{[0 0]} #{} 10 10 nil)
          entity (player/create-player-entity {:x 0 :y 0})
          result (pc/process-movement @stage entity :move-up)]
      (is (= :blocked (:status result)))
      (is (= "out-of-bounds" (:reason result))))))

(deftest process-non-movement-action
  (testing "Non-movement action returns appropriate result"
    (let [stage (env/create_stage "test" #{[5 5]} #{} 10 10 nil)
          entity (player/create-player-entity {:x 5 :y 5})
          result (pc/process-action @stage entity :wait)]
      (is (= :no-move (:status result)))
      (is (= :wait (:action result))))))

;; =============================================================================
;; Control State Updates Tests
;; =============================================================================

(deftest update-control-state
  (testing "Update control with successful action"
    (let [ctrl (pc/create-control)
          updated (pc/record-action ctrl :move-up :success)]
      (is (= :move-up (:last-action updated)))
      (is (= 1 (:action-count updated)))
      (is (= :success (:last-result updated)))))
  (testing "Track multiple actions"
    (let [ctrl (-> (pc/create-control)
                   (pc/record-action :move-up :success)
                   (pc/record-action :move-right :success))]
      (is (= :move-right (:last-action ctrl)))
      (is (= 2 (:action-count ctrl))))))

;; =============================================================================
;; Full Integration Tests
;; =============================================================================

(deftest full-player-control-cycle
  (testing "Complete input to movement validation cycle"
    (let [stage (env/create_stage "test" #{[5 5] [5 4] [6 5]} #{} 10 10 nil)
          entity (player/create-player-entity {:x 5 :y 5})
          ctrl (pc/create-control)]
      ;; Process valid movement
      (let [result (pc/handle-input @stage entity ctrl :move-up)]
        (is (= :success (:status result)))
        (is (= [5 4] (:target result))))
      ;; Process movement to occupied space
      (let [blocker (collision/create-entity-with-collision {:x 6 :y 5 :solid true})]
        (env/add-entity stage blocker)
        (let [result (pc/handle-input @stage entity ctrl :move-right)]
          (is (= :blocked (:status result))))))))

(deftest player-control-with-disabled-state
  (testing "Disabled control rejects all input"
    (let [stage (env/create_stage "test" #{[5 5] [5 4]} #{} 10 10 nil)
          entity (player/create-player-entity {:x 5 :y 5})
          ctrl (pc/create-control {:enabled false})
          result (pc/handle-input @stage entity ctrl :move-up)]
      (is (= :disabled (:status result)))
      (is (nil? (:target result))))))

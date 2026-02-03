(ns components.player-test
  (:require [clojure.test :refer :all]
            [components.player :as player]
            [components.collision :as collision]
            [actors.agent :as agent]
            [stage.environment :as env]))

;; =============================================================================
;; Player Component Creation Tests
;; =============================================================================

(deftest create-player-component
  (testing "Create a player component with defaults"
    (let [pc (player/create-player-component)]
      (is (map? pc))
      (is (some? (:player-id pc)))
      (is (= \@ (:display-char pc)))
      (is (= 1 (:speed pc)))
      (is (true? (:controllable pc))))))

(deftest create-player-component-custom
  (testing "Create a player component with custom values"
    (let [pc (player/create-player-component {:player-id "player-1"
                                               :display-char \*
                                               :speed 2})]
      (is (= "player-1" (:player-id pc)))
      (is (= \* (:display-char pc)))
      (is (= 2 (:speed pc))))))

;; =============================================================================
;; Entity Player Management Tests
;; =============================================================================

(deftest add-player-to-entity
  (testing "Add player component to an entity"
    (let [entity (agent/_entity)
          pc (player/create-player-component)
          updated (player/add-player-component entity pc)]
      (is (some? (player/get-player-component updated)))
      (is (player/player? updated)))))

(deftest non-player-entity
  (testing "Entity without player component is not a player"
    (let [entity (agent/_entity)]
      (is (not (player/player? entity)))
      (is (nil? (player/get-player-component entity))))))

(deftest get-display-char
  (testing "Get display character for player entity"
    (let [entity (-> (agent/_entity)
                     (player/add-player-component 
                      (player/create-player-component {:display-char \X})))]
      (is (= \X (player/get-display-char entity))))))

;; =============================================================================
;; Player Entity Factory Tests
;; =============================================================================

(deftest create-complete-player-entity
  (testing "Create a complete player entity with position"
    (let [p (player/create-player-entity {:x 10 :y 20})]
      (is (player/player? p))
      (is (collision/has-collision? p))
      (is (= 10 (:x (collision/get-collision p))))
      (is (= 20 (:y (collision/get-collision p))))
      (is (= :player (:type p))))))

(deftest player-entity-custom-char
  (testing "Create player entity with custom display character"
    (let [p (player/create-player-entity {:x 5 :y 5 :display-char \#})]
      (is (= \# (player/get-display-char p))))))

(deftest get-player-position
  (testing "Get player position as vector"
    (let [p (player/create-player-entity {:x 15 :y 25})]
      (is (= [15 25] (player/get-player-position p))))))

;; =============================================================================
;; Player Query Tests
;; =============================================================================

(deftest find-player-in-stage
  (testing "Find player entity in a stage"
    (let [stage (env/create_stage "test" #{} #{} nil nil nil)
          p (player/create-player-entity {:x 5 :y 5})]
      (env/add-agent stage p)
      (let [found (player/find-player @stage)]
        (is (some? found))
        (is (= (:id p) (:id found)))))))

(deftest find-player-returns-nil-when-none
  (testing "find-player returns nil when no player exists"
    (let [stage (env/create_stage "test" #{} #{} nil nil nil)
          entity (agent/_entity)]
      (env/add-entity stage entity)
      (is (nil? (player/find-player @stage))))))

(deftest find-all-players
  (testing "Find multiple players in a stage"
    (let [stage (env/create_stage "test" #{} #{} nil nil nil)
          p1 (player/create-player-entity {:x 5 :y 5 :player-id "p1"})
          p2 (player/create-player-entity {:x 10 :y 10 :player-id "p2"})]
      (env/add-agent stage p1)
      (env/add-agent stage p2)
      (let [players (player/find-all-players @stage)]
        (is (= 2 (count players)))))))

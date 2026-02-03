(ns components.collision-test
  (:require [clojure.test :refer :all]
            [components.collision :as collision]
            [components.properties :as props]
            [actors.agent :as agent]
            [stage.environment :as env]))

;; =============================================================================
;; Collision Component Creation Tests
;; =============================================================================

(deftest create-collision-component
  (testing "Create a basic collision component with position and bounds"
    (let [comp (collision/create-collision {:x 5 :y 10 :width 1 :height 1 :solid true})]
      (is (map? comp))
      (is (= 5 (:x comp)))
      (is (= 10 (:y comp)))
      (is (= 1 (:width comp)))
      (is (= 1 (:height comp)))
      (is (true? (:solid comp))))))

(deftest create-collision-with-defaults
  (testing "Collision component has sensible defaults"
    (let [comp (collision/create-collision {:x 0 :y 0})]
      (is (= 1 (:width comp)))   ;; default 1x1 tile
      (is (= 1 (:height comp)))
      (is (true? (:solid comp))))))  ;; solid by default

(deftest create-passable-collision
  (testing "Create a non-solid/passable collision component (for triggers, zones)"
    (let [comp (collision/create-collision {:x 3 :y 7 :solid false})]
      (is (false? (:solid comp))))))

;; =============================================================================
;; Entity with Collision Tests
;; =============================================================================

(deftest add-collision-to-agent
  (testing "Add collision component to an agent entity"
    (let [ag (agent/_entity)
          collision-comp (collision/create-collision {:x 10 :y 20 :solid true})
          updated (collision/add-collision ag collision-comp)]
      (is (some? (collision/get-collision updated)))
      (is (= 10 (:x (collision/get-collision updated))))
      (is (= 20 (:y (collision/get-collision updated)))))))

(deftest add-collision-to-scenery
  (testing "Add collision component to scenery (wall, obstacle)"
    (let [wall (agent/_entity)
          wall-with-props (props/create_entity 
                           :entity wall 
                           :components {:type :wall :destructible false})
          collision-comp (collision/create-collision {:x 5 :y 5 :width 1 :height 1 :solid true})
          updated (collision/add-collision wall-with-props collision-comp)]
      (is (collision/solid? updated))
      (is (= {:x 5 :y 5 :width 1 :height 1} (collision/get-bounds updated))))))

(deftest update-collision-position
  (testing "Update an entity's collision position (for movement)"
    (let [ag (agent/_entity)
          collision-comp (collision/create-collision {:x 0 :y 0 :solid true})
          with-collision (collision/add-collision ag collision-comp)
          moved (collision/set-position with-collision 15 25)]
      (is (= 15 (:x (collision/get-collision moved))))
      (is (= 25 (:y (collision/get-collision moved)))))))

;; =============================================================================
;; Collision Detection Tests
;; =============================================================================

(deftest point-collision-check
  (testing "Check if a point collides with an entity's collision bounds"
    (let [entity (-> (agent/_entity)
                     (collision/add-collision 
                      (collision/create-collision {:x 5 :y 5 :width 3 :height 3 :solid true})))]
      ;; Point inside bounds (5,5 to 7,7)
      (is (collision/point-in-bounds? 6 6 entity))
      (is (collision/point-in-bounds? 5 5 entity))
      (is (collision/point-in-bounds? 7 7 entity))
      ;; Point outside bounds
      (is (not (collision/point-in-bounds? 4 5 entity)))
      (is (not (collision/point-in-bounds? 8 8 entity)))
      (is (not (collision/point-in-bounds? 0 0 entity))))))

(deftest entity-overlap-check
  (testing "Check if two entities' collision bounds overlap"
    (let [entity-a (-> (agent/_entity)
                       (collision/add-collision 
                        (collision/create-collision {:x 0 :y 0 :width 3 :height 3 :solid true})))
          entity-b (-> (agent/_entity)
                       (collision/add-collision 
                        (collision/create-collision {:x 2 :y 2 :width 3 :height 3 :solid true})))
          entity-c (-> (agent/_entity)
                       (collision/add-collision 
                        (collision/create-collision {:x 10 :y 10 :width 2 :height 2 :solid true})))]
      ;; A and B overlap (both occupy 2,2)
      (is (collision/overlaps? entity-a entity-b))
      (is (collision/overlaps? entity-b entity-a))
      ;; A and C do not overlap
      (is (not (collision/overlaps? entity-a entity-c)))
      ;; B and C do not overlap
      (is (not (collision/overlaps? entity-b entity-c))))))

(deftest passable-entities-dont-block
  (testing "Non-solid entities report as passable even when overlapping"
    (let [trigger-zone (-> (agent/_entity)
                           (collision/add-collision 
                            (collision/create-collision {:x 5 :y 5 :width 5 :height 5 :solid false})))
          player (-> (agent/_entity)
                     (collision/add-collision 
                      (collision/create-collision {:x 7 :y 7 :width 1 :height 1 :solid true})))]
      ;; They overlap geometrically
      (is (collision/overlaps? trigger-zone player))
      ;; But movement is not blocked because trigger is passable
      (is (not (collision/blocks-movement? trigger-zone player))))))

;; =============================================================================
;; Stage Integration Tests
;; =============================================================================

(deftest entities-with-collision-in-stage
  (testing "Add entities with collision to a stage and query by position"
    (let [stage (env/create_stage "collision-test" #{} #{} nil nil nil)
          wall (-> (agent/_entity)
                   (collision/add-collision 
                    (collision/create-collision {:x 10 :y 10 :width 1 :height 1 :solid true})))
          player (-> (agent/_entity)
                     (collision/add-collision 
                      (collision/create-collision {:x 5 :y 5 :width 1 :height 1 :solid true})))]
      (env/add-entity stage wall)
      (env/add-agent stage player)
      ;; Verify entities are in stage
      (is (= wall (get-in @stage [:entities (:id wall)])))
      (is (contains? (:agent_pool @stage) player)))))

(deftest find-entities-at-position
  (testing "Find all entities occupying a given tile position"
    (let [stage (env/create_stage "query-test" #{} #{} nil nil nil)
          wall1 (-> (agent/_entity)
                    (collision/add-collision 
                     (collision/create-collision {:x 5 :y 5 :solid true})))
          wall2 (-> (agent/_entity)
                    (collision/add-collision 
                     (collision/create-collision {:x 5 :y 6 :solid true})))
          floor (-> (agent/_entity)
                    (collision/add-collision 
                     (collision/create-collision {:x 5 :y 5 :solid false})))]
      (env/add-entity stage wall1)
      (env/add-entity stage wall2)
      (env/add-entity stage floor)
      (let [entities-at-5-5 (collision/entities-at-position @stage 5 5)]
        (is (= 2 (count entities-at-5-5)))  ;; wall1 and floor
        (is (some #(= (:id %) (:id wall1)) entities-at-5-5))
        (is (some #(= (:id %) (:id floor)) entities-at-5-5))))))

(deftest check-tile-walkable
  (testing "Check if a tile position is walkable (no solid entities blocking)"
    (let [stage (env/create_stage "walkable-test" #{} #{} nil nil nil)
          wall (-> (agent/_entity)
                   (collision/add-collision 
                    (collision/create-collision {:x 3 :y 3 :solid true})))
          trigger (-> (agent/_entity)
                      (collision/add-collision 
                       (collision/create-collision {:x 4 :y 4 :solid false})))]
      (env/add-entity stage wall)
      (env/add-entity stage trigger)
      ;; Tile with solid wall is not walkable
      (is (not (collision/tile-walkable? @stage 3 3)))
      ;; Tile with only trigger (passable) is walkable
      (is (collision/tile-walkable? @stage 4 4))
      ;; Empty tile is walkable
      (is (collision/tile-walkable? @stage 10 10)))))

;; =============================================================================
;; Wall/Scenery Type Tests
;; =============================================================================

(deftest wall-entity-creation
  (testing "Create a wall entity with collision"
    (let [wall (collision/create-wall 5 10)]
      (is (collision/solid? wall))
      (is (= 5 (:x (collision/get-collision wall))))
      (is (= 10 (:y (collision/get-collision wall))))
      (is (= :wall (get-in wall [:type]))))))

(deftest floor-entity-creation
  (testing "Create a floor entity (passable)"
    (let [floor (collision/create-floor 7 8)]
      (is (not (collision/solid? floor)))
      (is (= 7 (:x (collision/get-collision floor))))
      (is (= 8 (:y (collision/get-collision floor))))
      (is (= :floor (get-in floor [:type]))))))

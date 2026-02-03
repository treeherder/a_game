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
;; Solid Entity Collision Constraint Tests
;; =============================================================================

(deftest solid-entities-cannot-share-tile
  (testing "Two solid entities cannot occupy the same tile position"
    (let [stage (env/create_stage "collision-test" #{} #{} nil nil nil)
          wall (collision/create-wall 5 5)
          agent-entity (-> (agent/_entity)
                           (collision/add-collision 
                            (collision/create-collision {:x 5 :y 5 :solid true})))]
      ;; Add wall first
      (env/add-entity stage wall)
      ;; Attempt to add agent at same position
      (env/add-agent stage agent-entity)
      ;; The tile should not be walkable (blocked by wall)
      (is (not (collision/tile-walkable? @stage 5 5)))
      ;; Both entities exist but they're violating collision constraint
      (let [entities-at-pos (collision/entities-at-position @stage 5 5)]
        (is (>= (count entities-at-pos) 2))
        ;; At least one solid entity blocks the position
        (is (not (empty? (collision/solid-entities-at-position @stage 5 5))))))))

(deftest agent-blocked-by-existing-agent
  (testing "Agent cannot move to tile occupied by another solid agent"
    (let [stage (env/create_stage "agent-collision" #{} #{} nil nil nil)
          agent1 (-> (agent/_entity)
                     (collision/add-collision 
                      (collision/create-collision {:x 5 :y 5 :solid true})))
          agent2 (-> (agent/_entity)
                     (collision/add-collision 
                      (collision/create-collision {:x 6 :y 5 :solid true})))]
      (env/add-agent stage agent1)
      (env/add-agent stage agent2)
      ;; Position (5,5) has agent1 - not walkable
      (is (not (collision/tile-walkable? @stage 5 5)))
      ;; Position (6,5) has agent2 - not walkable
      (is (not (collision/tile-walkable? @stage 6 5)))
      ;; Empty position (7,5) should be walkable
      (is (collision/tile-walkable? @stage 7 5)))))

(deftest scenery-blocks-agent-movement
  (testing "Scenery (walls, obstacles) blocks agent movement"
    (let [stage (env/create_stage "scenery-blocks" #{} #{} nil nil nil)
          obstacle (-> (agent/_entity)
                       (assoc :type :obstacle)
                       (collision/add-collision 
                        (collision/create-collision {:x 10 :y 10 :width 2 :height 2 :solid true})))
          agent-entity (-> (agent/_entity)
                           (collision/add-collision 
                            (collision/create-collision {:x 8 :y 10 :solid true})))]
      (env/add-entity stage obstacle)
      (env/add-agent stage agent-entity)
      ;; Obstacle blocks (10,10), (11,10), (10,11), (11,11)
      (is (not (collision/tile-walkable? @stage 10 10)))
      (is (not (collision/tile-walkable? @stage 11 10)))
      (is (not (collision/tile-walkable? @stage 10 11)))
      (is (not (collision/tile-walkable? @stage 11 11)))
      ;; Agent's position is blocked
      (is (not (collision/tile-walkable? @stage 8 10)))
      ;; Adjacent empty tile is walkable
      (is (collision/tile-walkable? @stage 9 10)))))

(deftest passable-entities-allow-overlap
  (testing "Multiple passable (non-solid) entities can occupy same tile"
    (let [stage (env/create_stage "passable-overlap" #{} #{} nil nil nil)
          trigger1 (collision/create-floor 5 5)  ;; floor is passable
          trigger2 (-> (agent/_entity)
                       (assoc :type :trigger)
                       (collision/add-collision 
                        (collision/create-collision {:x 5 :y 5 :solid false})))]
      (env/add-entity stage trigger1)
      (env/add-entity stage trigger2)
      ;; Multiple passable entities at same position
      (let [entities-at-pos (collision/entities-at-position @stage 5 5)]
        (is (= 2 (count entities-at-pos))))
      ;; Tile is still walkable because all entities are passable
      (is (collision/tile-walkable? @stage 5 5)))))

(deftest mixed-solid-passable-blocks-movement
  (testing "Tile with both solid and passable entities is not walkable"
    (let [stage (env/create_stage "mixed-entities" #{} #{} nil nil nil)
          floor (collision/create-floor 5 5)  ;; passable
          wall (collision/create-wall 5 5)]   ;; solid
      (env/add-entity stage floor)
      (env/add-entity stage wall)
      ;; Even though floor is passable, wall blocks movement
      (is (not (collision/tile-walkable? @stage 5 5)))
      ;; Both entities exist at position
      (let [entities-at-pos (collision/entities-at-position @stage 5 5)
            solid-count (count (collision/solid-entities-at-position @stage 5 5))]
        (is (= 2 (count entities-at-pos)))
        (is (= 1 solid-count))))))

(deftest validate-position-before-placement
  (testing "Check if position is valid before placing entity"
    (let [stage (env/create_stage "validate-placement" #{} #{} nil nil nil)
          existing-wall (collision/create-wall 5 5)]
      (env/add-entity stage existing-wall)
      ;; Position (5,5) should not be walkable
      (is (not (collision/tile-walkable? @stage 5 5)))
      ;; Position (6,6) should be walkable (empty)
      (is (collision/tile-walkable? @stage 6 6))
      ;; Can use tile-walkable? to validate before placement
      (let [new-entity (collision/create-wall 6 6)]
        (when (collision/tile-walkable? @stage 6 6)
          (env/add-entity stage new-entity))
        ;; Now (6,6) should not be walkable
        (is (not (collision/tile-walkable? @stage 6 6)))))))

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

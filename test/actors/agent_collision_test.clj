(ns actors.agent-collision-test
  (:require [clojure.test :refer :all]
            [actors.agent :as a]
            [stage.environment :as env]
            [components.collision :as collision]
            [components.properties :as props]))

;; =============================================================================
;; Basic Agent Tests
;; =============================================================================

(deftest agent-pool-returns-seq
  (let [st (env/create_stage "s" #{} #{} nil nil nil)
        ag (a/_entity)]
    (env/add-agent st ag)
    (is (seq? (a/agent_pool st)))))

;; =============================================================================
;; BODY/COLLISION AS UNIVERSAL COMPONENT
;; =============================================================================
;;
;; Any entity with physicality should have options for a BODY keymap.
;; Many objects and agents will have bodies that are represented similarly to stages.
;; Each of these bodies has a set of unique, sometimes attached entities -- limbs, appendages.
;; In some cases, these appendage entities might be groups of other, similar entities.
;; Arms are made of upper and lower segments, hands, shoulder, elbow, and wrist joints.
;; Hands are built from fingers, etc.
;; Additionally, each of these entities has constituent components and behaviors that need to
;; be relayed up the top level of the data structure.
;;
;; Collision is the BASIS for all physical entities - it defines:
;; - Position in world space
;; - Bounds/dimensions
;; - Solid vs passable behavior
;; =============================================================================

;; -----------------------------------------------------------------------------
;; Test: Every entity with a body must have a collision component
;; -----------------------------------------------------------------------------

(deftest entity-with-body-requires-collision
  (testing "An entity representing a physical body must have collision"
    (let [body-entity (-> (a/_entity)
                          (collision/add-collision
                           (collision/create-collision {:x 0 :y 0 :width 1 :height 2 :solid true})))]
      (is (collision/has-collision? body-entity))
      (is (= {:x 0 :y 0 :width 1 :height 2} (collision/get-bounds body-entity))))))

(deftest entity-without-collision-has-no-physicality
  (testing "An entity without collision has no physical presence"
    (let [abstract-entity (a/_entity)]
      (is (not (collision/has-collision? abstract-entity)))
      (is (nil? (collision/get-bounds abstract-entity)))
      (is (not (collision/solid? abstract-entity))))))

;; -----------------------------------------------------------------------------
;; Test: Body parts as sub-entities with their own collision bounds
;; -----------------------------------------------------------------------------

(defn create-body-part
  "Create a body part entity with collision relative to parent position.
   Body parts are sub-entities that can have their own collision bounds."
  [part-type x y width height & {:keys [solid attached-to]
                                  :or {solid false attached-to nil}}]
  (-> (a/_entity)
      (assoc :type :body-part
             :part-type part-type
             :attached-to attached-to)
      (collision/add-collision
       (collision/create-collision {:x x :y y :width width :height height :solid solid}))))

(deftest body-parts-have-individual-collision
  (testing "Each body part is an entity with its own collision bounds"
    (let [head (create-body-part :head 5 10 1 1 :solid true)
          torso (create-body-part :torso 5 11 1 2 :solid true)
          left-arm (create-body-part :arm 4 11 1 2 :solid false)
          right-arm (create-body-part :arm 6 11 1 2 :solid false)]
      ;; Each part has collision
      (is (collision/has-collision? head))
      (is (collision/has-collision? torso))
      (is (collision/has-collision? left-arm))
      (is (collision/has-collision? right-arm))
      ;; Solid parts block movement
      (is (collision/solid? head))
      (is (collision/solid? torso))
      ;; Limbs might be passable for certain interactions
      (is (not (collision/solid? left-arm)))
      (is (not (collision/solid? right-arm))))))

;; -----------------------------------------------------------------------------
;; Test: Composite body as a collection of body parts
;; -----------------------------------------------------------------------------

(defn create-humanoid-body
  "Create a humanoid body as a map of body parts with collision.
   Returns {:core <main-entity> :parts [list of body part entities]}"
  [base-x base-y]
  (let [core (-> (a/_entity)
                 (assoc :type :humanoid-body)
                 (collision/add-collision
                  (collision/create-collision {:x base-x :y base-y :width 1 :height 3 :solid true})))
        head (create-body-part :head base-x base-y 1 1 :solid true :attached-to (:id core))
        torso (create-body-part :torso base-x (+ base-y 1) 1 1 :solid true :attached-to (:id core))
        legs (create-body-part :legs base-x (+ base-y 2) 1 1 :solid true :attached-to (:id core))]
    {:core core
     :parts [head torso legs]}))

(deftest composite-body-collision-bounds
  (testing "A composite body's main collision encompasses its parts"
    (let [body (create-humanoid-body 10 20)]
      ;; Core entity has bounds covering all parts (1x3 at position 10,20)
      (is (= {:x 10 :y 20 :width 1 :height 3} (collision/get-bounds (:core body))))
      ;; Has 3 body parts
      (is (= 3 (count (:parts body))))
      ;; Each part is attached to core
      (is (every? #(= (:id (:core body)) (:attached-to %)) (:parts body))))))

(deftest body-parts-tracked-by-parent
  (testing "Body parts can reference their parent entity"
    (let [body (create-humanoid-body 5 5)
          head (first (:parts body))]
      (is (= :head (:part-type head)))
      (is (= (:id (:core body)) (:attached-to head))))))

;; -----------------------------------------------------------------------------
;; Test: Moving a body moves its collision bounds
;; -----------------------------------------------------------------------------

(deftest move-body-updates-collision-position
  (testing "When an entity moves, its collision position updates"
    (let [entity (-> (a/_entity)
                     (collision/add-collision
                      (collision/create-collision {:x 0 :y 0 :solid true})))
          moved (collision/set-position entity 10 15)]
      (is (= 10 (:x (collision/get-collision moved))))
      (is (= 15 (:y (collision/get-collision moved)))))))

(defn move-body-with-parts
  "Move a composite body and all its parts by a delta."
  [{:keys [core parts] :as body} dx dy]
  (let [move-entity (fn [e]
                      (let [coll (collision/get-collision e)]
                        (collision/set-position e (+ (:x coll) dx) (+ (:y coll) dy))))]
    {:core (move-entity core)
     :parts (mapv move-entity parts)}))

(deftest move-composite-body
  (testing "Moving a composite body moves all parts together"
    (let [body (create-humanoid-body 0 0)
          moved-body (move-body-with-parts body 5 10)]
      ;; Core moved to 5,10
      (is (= {:x 5 :y 10 :width 1 :height 3} (collision/get-bounds (:core moved-body))))
      ;; Head at 5,10
      (is (= 5 (:x (collision/get-collision (first (:parts moved-body))))))
      (is (= 10 (:y (collision/get-collision (first (:parts moved-body))))))
      ;; Torso at 5,11
      (is (= 5 (:x (collision/get-collision (second (:parts moved-body))))))
      (is (= 11 (:y (collision/get-collision (second (:parts moved-body)))))))))

;; -----------------------------------------------------------------------------
;; Test: Collision between bodies
;; -----------------------------------------------------------------------------

(deftest two-bodies-collide
  (testing "Two bodies occupying the same space collide"
    (let [body-a (create-humanoid-body 5 5)
          body-b (create-humanoid-body 5 6)]  ; overlaps with body-a (5,5 to 5,7 vs 5,6 to 5,8)
      (is (collision/overlaps? (:core body-a) (:core body-b))))))

(deftest bodies-at-different-positions-no-collision
  (testing "Two bodies at different positions don't collide"
    (let [body-a (create-humanoid-body 0 0)
          body-b (create-humanoid-body 10 10)]
      (is (not (collision/overlaps? (:core body-a) (:core body-b)))))))

(deftest adjacent-bodies-no-collision
  (testing "Two bodies adjacent to each other don't collide"
    (let [body-a (create-humanoid-body 0 0)   ; occupies 0,0 to 0,2
          body-b (create-humanoid-body 1 0)]  ; occupies 1,0 to 1,2 (side by side)
      (is (not (collision/overlaps? (:core body-a) (:core body-b)))))))

;; -----------------------------------------------------------------------------
;; Test: Body parts collision (fine-grained collision detection)
;; -----------------------------------------------------------------------------

(defn get-colliding-parts
  "Find all body parts from body-b that collide with body-a's core or parts."
  [body-a body-b]
  (let [all-a (cons (:core body-a) (:parts body-a))
        all-b (cons (:core body-b) (:parts body-b))]
    (for [part-a all-a
          part-b all-b
          :when (and (not= (:id part-a) (:id part-b))
                     (collision/overlaps? part-a part-b))]
      {:part-a (:part-type part-a)
       :part-b (:part-type part-b)})))

(deftest fine-grained-body-part-collision
  (testing "Detect which specific body parts are colliding"
    (let [body-a (create-humanoid-body 5 5)    ; head at 5,5, torso at 5,6, legs at 5,7
          body-b (create-humanoid-body 5 7)]   ; head at 5,7 (collides with body-a legs)
      ;; The cores overlap
      (is (collision/overlaps? (:core body-a) (:core body-b)))
      ;; Find specific colliding parts
      (let [collisions (get-colliding-parts body-a body-b)]
        ;; Should have collisions involving legs of A and head of B
        (is (pos? (count collisions)))))))

;; -----------------------------------------------------------------------------
;; Test: Bodies in stage world
;; -----------------------------------------------------------------------------

(deftest add-body-to-stage
  (testing "Add a body entity to the stage world"
    (let [stage (env/create_stage "body-test" #{} #{} nil nil nil)
          body (create-humanoid-body 10 10)]
      ;; Add core as agent, parts as entities
      (env/add-agent stage (:core body))
      (doseq [part (:parts body)]
        (env/add-entity stage part))
      ;; Verify core is in agent pool
      (is (contains? (:agent_pool @stage) (:core body)))
      ;; Verify parts are in entities
      (is (= 3 (count (:entities @stage)))))))

(deftest query-bodies-at-position
  (testing "Query which bodies/parts occupy a tile position"
    (let [stage (env/create_stage "query-test" #{} #{} nil nil nil)
          body (create-humanoid-body 5 5)]
      (env/add-agent stage (:core body))
      (doseq [part (:parts body)]
        (env/add-entity stage part))
      ;; Position 5,5 should have entities (head is at 5,5)
      (let [at-5-5 (collision/entities-at-position @stage 5 5)]
        (is (pos? (count at-5-5))))
      ;; Position 5,7 should have entities (legs at 5,7)
      (let [at-5-7 (collision/entities-at-position @stage 5 7)]
        (is (pos? (count at-5-7))))
      ;; Position 0,0 should be empty
      (let [at-0-0 (collision/entities-at-position @stage 0 0)]
        (is (empty? at-0-0))))))

;; -----------------------------------------------------------------------------
;; Test: Different body types with different collision profiles
;; -----------------------------------------------------------------------------

(defn create-large-creature
  "Create a large creature with 2x2 collision bounds."
  [x y]
  (-> (a/_entity)
      (assoc :type :large-creature)
      (collision/add-collision
       (collision/create-collision {:x x :y y :width 2 :height 2 :solid true}))))

(defn create-small-creature
  "Create a small creature with 1x1 collision bounds."
  [x y]
  (-> (a/_entity)
      (assoc :type :small-creature)
      (collision/add-collision
       (collision/create-collision {:x x :y y :width 1 :height 1 :solid true}))))

(deftest different-sized-bodies
  (testing "Bodies of different sizes have appropriate collision bounds"
    (let [large (create-large-creature 0 0)
          small (create-small-creature 5 5)]
      (is (= {:x 0 :y 0 :width 2 :height 2} (collision/get-bounds large)))
      (is (= {:x 5 :y 5 :width 1 :height 1} (collision/get-bounds small))))))

(deftest large-body-blocks-more-tiles
  (testing "A larger body blocks more tile positions"
    (let [large (create-large-creature 5 5)]  ; occupies 5,5 to 6,6
      ;; All 4 tiles should register the entity
      (is (collision/point-in-bounds? 5 5 large))
      (is (collision/point-in-bounds? 6 5 large))
      (is (collision/point-in-bounds? 5 6 large))
      (is (collision/point-in-bounds? 6 6 large))
      ;; Adjacent tiles should not
      (is (not (collision/point-in-bounds? 4 5 large)))
      (is (not (collision/point-in-bounds? 7 5 large))))))

;; -----------------------------------------------------------------------------
;; Test: Ghost/Ethereal entities (non-solid but positioned)
;; -----------------------------------------------------------------------------

(defn create-ghost-entity
  "Create an ethereal entity that has position but doesn't block movement."
  [x y]
  (-> (a/_entity)
      (assoc :type :ghost)
      (collision/add-collision
       (collision/create-collision {:x x :y y :width 1 :height 1 :solid false}))))

(deftest ghost-has-position-but-no-solidity
  (testing "Ghost entities have collision bounds but don't block movement"
    (let [ghost (create-ghost-entity 5 5)
          player (create-small-creature 5 5)]
      ;; Ghost has collision component and position
      (is (collision/has-collision? ghost))
      (is (= {:x 5 :y 5 :width 1 :height 1} (collision/get-bounds ghost)))
      ;; Ghost is not solid
      (is (not (collision/solid? ghost)))
      ;; Ghost and player geometrically overlap
      (is (collision/overlaps? ghost player))
      ;; But ghost doesn't block player's movement
      (is (not (collision/blocks-movement? ghost player))))))

;; -----------------------------------------------------------------------------
;; Test: Entity collision as prerequisite for interaction
;; -----------------------------------------------------------------------------

(deftest entities-must-overlap-to-interact
  (testing "Two entities must have overlapping collision to interact"
    (let [entity-a (create-small-creature 0 0)
          entity-b (create-small-creature 0 0)  ; same position
          entity-c (create-small-creature 10 10)]  ; far away
      ;; A and B can interact (same position)
      (is (collision/overlaps? entity-a entity-b))
      ;; A and C cannot interact directly (too far)
      (is (not (collision/overlaps? entity-a entity-c))))))

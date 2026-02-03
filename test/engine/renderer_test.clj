(ns engine.renderer-test
  (:require [clojure.test :refer :all]
            [engine.renderer :as renderer]
            [components.player :as player]
            [components.collision :as collision]))

;; =============================================================================
;; Tile Character Tests
;; =============================================================================

(deftest get-tile-char-wall
  (testing "Wall tiles render as #"
    (let [walls #{[0 0] [1 1]}
          stage-state {:walls walls :walkable #{} :entities {} :agent_pool []}]
      (is (= \# (renderer/get-tile-char stage-state 0 0)))
      (is (= \# (renderer/get-tile-char stage-state 1 1))))))

(deftest get-tile-char-floor
  (testing "Walkable tiles render as ."
    (let [walkable #{[1 1] [2 2]}
          stage-state {:walls #{} :walkable walkable :entities {} :agent_pool []}]
      (is (= \. (renderer/get-tile-char stage-state 1 1)))
      (is (= \. (renderer/get-tile-char stage-state 2 2))))))

(deftest get-tile-char-void
  (testing "Non-walkable, non-wall tiles render as space"
    (let [stage-state {:walls #{} :walkable #{} :entities {} :agent_pool []}]
      (is (= \space (renderer/get-tile-char stage-state 5 5))))))

(deftest get-tile-char-player
  (testing "Player entity renders as @"
    (let [p (player/create-player-entity {:x 3 :y 3})
          stage-state {:walls #{} :walkable #{[3 3]} :entities {} :agent_pool [p]}]
      (is (= \@ (renderer/get-tile-char stage-state 3 3))))))

(deftest get-tile-char-custom-player
  (testing "Player with custom char renders correctly"
    (let [p (player/create-player-entity {:x 3 :y 3 :display-char \*})
          stage-state {:walls #{} :walkable #{[3 3]} :entities {} :agent_pool [p]}]
      (is (= \* (renderer/get-tile-char stage-state 3 3))))))

;; =============================================================================
;; Row Rendering Tests
;; =============================================================================

(deftest render-row
  (testing "Render a single row as string"
    (let [walkable #{[0 0] [1 0] [2 0]}
          walls #{[3 0]}
          stage-state {:walls walls :walkable walkable :entities {} :agent_pool [] :width 4}
          row (renderer/render-row stage-state 0 4)]
      (is (string? row))
      (is (= "...#" row)))))

(deftest render-row-with-player
  (testing "Render row containing player"
    (let [p (player/create-player-entity {:x 1 :y 0})
          walkable #{[0 0] [1 0] [2 0]}
          stage-state {:walls #{} :walkable walkable :entities {} :agent_pool [p] :width 3}
          row (renderer/render-row stage-state 0 3)]
      (is (= ".@." row)))))

;; =============================================================================
;; Full Stage Rendering Tests
;; =============================================================================

(deftest render-small-stage
  (testing "Render a small stage"
    (let [walkable #{[1 1]}
          walls #{[0 0] [1 0] [2 0]
                  [0 1]       [2 1]
                  [0 2] [1 2] [2 2]}
          stage-state {:walls walls :walkable walkable 
                       :entities {} :agent_pool []
                       :width 3 :height 3}
          rendered (renderer/render-stage stage-state)]
      (is (string? rendered))
      (is (.contains rendered "#.#")))))

(deftest render-stage-with-player
  (testing "Render stage with player"
    (let [p (player/create-player-entity {:x 1 :y 1})
          walkable #{[1 1]}
          walls #{[0 0] [1 0] [2 0]
                  [0 1]       [2 1]
                  [0 2] [1 2] [2 2]}
          stage-state {:walls walls :walkable walkable 
                       :entities {} :agent_pool [p]
                       :width 3 :height 3}
          rendered (renderer/render-stage stage-state)]
      (is (.contains rendered "@")))))

;; =============================================================================
;; Viewport Rendering Tests
;; =============================================================================

(deftest render-viewport-centered
  (testing "Render viewport centered on position"
    (let [walkable (set (for [x (range 20) y (range 20)] [x y]))
          p (player/create-player-entity {:x 10 :y 10})
          stage-state {:walls #{} :walkable walkable 
                       :entities {} :agent_pool [p]
                       :width 20 :height 20}
          rendered (renderer/render-viewport stage-state 10 10 5 5)]
      (is (string? rendered))
      ;; Should contain player
      (is (.contains rendered "@"))
      ;; Should be limited in size (5 rows)
      (let [lines (clojure.string/split-lines rendered)]
        (is (= 5 (count lines)))))))

;; =============================================================================
;; Status Rendering Tests
;; =============================================================================

(deftest render-status-line
  (testing "Render status with player info"
    (let [p (player/create-player-entity {:x 5 :y 10})
          stage-state {:walls #{} :walkable #{} :entities {} :agent_pool [p]}
          clock {:tick 42}
          status (renderer/render-status stage-state clock)]
      (is (string? status))
      (is (.contains status "5"))
      (is (.contains status "10"))
      (is (.contains status "42")))))

;; =============================================================================
;; Entity Character Tests
;; =============================================================================

(deftest get-entity-char-player
  (testing "Player entities return @"
    (let [p (player/create-player-entity {:x 0 :y 0})]
      (is (= \@ (renderer/get-entity-char p))))))

(deftest get-entity-char-custom
  (testing "Player with custom char"
    (let [p (player/create-player-entity {:x 0 :y 0 :display-char \P})]
      (is (= \P (renderer/get-entity-char p))))))

(deftest get-entity-char-wall-type
  (testing "Wall type entities return #"
    (let [wall {:type :wall}]
      (is (= \# (renderer/get-entity-char wall))))))

;; =============================================================================
;; Entity Rendering Priority Tests
;; =============================================================================

(deftest player-renders-over-floor
  (testing "Players are rendered over floors"
    (let [p (player/create-player-entity {:x 0 :y 0})
          stage-state {:walls #{} :walkable #{[0 0]} :entities {} :agent_pool [p]}]
      (is (= \@ (renderer/get-tile-char stage-state 0 0))))))

(deftest entities-at-position
  (testing "Find entities at a given position"
    (let [p (player/create-player-entity {:x 5 :y 5})
          stage-state {:walls #{} :walkable #{} :entities {} :agent_pool [p]}
          entities (renderer/entities-at stage-state 5 5)]
      (is (= 1 (count entities)))
      (is (player/player? (first entities))))))

(deftest no-entities-at-empty-position
  (testing "No entities at empty position"
    (let [p (player/create-player-entity {:x 5 :y 5})
          stage-state {:walls #{} :walkable #{} :entities {} :agent_pool [p]}
          entities (renderer/entities-at stage-state 0 0)]
      (is (empty? entities)))))

;; =============================================================================
;; Full Game Rendering Tests
;; =============================================================================

(deftest render-game-view
  (testing "Render complete game view with status"
    (let [p (player/create-player-entity {:x 5 :y 5})
          walkable (set (for [x (range 10) y (range 10)] [x y]))
          stage-state {:walls #{} :walkable walkable 
                       :entities {} :agent_pool [p]
                       :width 10 :height 10}
          clock {:tick 0}
          rendered (renderer/render-game stage-state clock 10 5)]
      (is (string? rendered))
      (is (.contains rendered "@"))
      (is (.contains rendered "Pos:")))))

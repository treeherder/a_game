(ns stagebuilder.generator-test
  (:require [clojure.test :refer :all]
            [stagebuilder.generator :as gen]))

(deftest generate-labyrinth-structure
  (testing "Generate an 8k x 8k labyrinth with rooms"
    (let [lab (gen/generate-labyrinth 8000 8000)]
      (is (= 8000 (:width lab)))
      (is (= 8000 (:height lab)))
      (is (>= (count (:rooms lab)) 5)))))

(deftest room-detection
  (testing "Point inside room detection"
    (let [room (gen/generate-room 10 10 20 15)]
      (is (gen/point-in-room? 15 15 room))
      (is (not (gen/point-in-room? 5 5 room)))
      (is (not (gen/point-in-room? 35 15 room))))))

(deftest wall-detection
  (testing "Point on wall detection"
    (let [room (gen/generate-room 10 10 20 15)]
      (is (gen/point-on-room-wall? 10 10 room))
      (is (gen/point-on-room-wall? 29 10 room))
      (is (gen/point-on-room-wall? 10 24 room))
      (is (not (gen/point-on-room-wall? 15 15 room))))))

(deftest tile-rendering
  (testing "Get tile at coordinate"
    (let [lab (gen/generate-labyrinth 200 200)]
      ;; Test that tiles are either walls or floors (valid characters)
      (is (or (= \# (gen/get-tile-at 2 2 lab))
              (= \space (gen/get-tile-at 2 2 lab))))
      ;; Check that we have both walls and floors in the map
      (is (> (count (:rooms lab)) 0))
      ;; Verify a room's interior has floor tiles (space character)
      (let [room (first (:rooms lab))
            center-x (+ (:x room) (quot (:width room) 2))
            center-y (+ (:y room) (quot (:height room) 2))]
        (is (= \space (gen/get-tile-at center-x center-y lab)))))))

(deftest render-small-labyrinth
  (testing "Render a small labyrinth"
    (let [lab {:width 50
               :height 30
               :rooms [(gen/generate-room 5 5 20 10)]}
          rendered (gen/render-labyrinth lab)
          lines (clojure.string/split-lines rendered)]
      (is (= 30 (count lines)))
      (is (every? #(= 50 (count %)) lines)))))

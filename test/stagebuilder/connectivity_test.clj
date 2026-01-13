(ns stagebuilder.connectivity-test
  (:require [clojure.test :refer :all]
            [stagebuilder.generator :as gen]))

(defn get-all-walkable-tiles
  "Get all walkable tiles (rooms + corridors)."
  [labyrinth]
  (let [room-tiles (set (for [room (:rooms labyrinth)
                              x (range (inc (:x room)) (+ (:x room) (dec (:width room))))
                              y (range (inc (:y room)) (+ (:y room) (dec (:height room))))]
                          [x y]))
        corridor-tiles (:corridors labyrinth)]
    (into room-tiles corridor-tiles)))

(defn get-neighbors
  "Get walkable neighbors of a tile."
  [x y walkable-set]
  (filter walkable-set
          [[(dec x) y] [(inc x) y] [x (dec y)] [x (inc y)]]))

(defn flood-fill
  "Flood fill from a starting position to find all connected tiles."
  [start-pos walkable-set]
  (loop [queue [start-pos]
         visited #{start-pos}]
    (if (empty? queue)
      visited
      (let [current (first queue)
            [x y] current
            neighbors (get-neighbors x y walkable-set)
            unvisited (filter #(not (contains? visited %)) neighbors)]
        (recur (concat (rest queue) unvisited)
               (into visited unvisited))))))

(deftest test-full-connectivity
  (testing "All rooms and corridors are fully connected"
    (let [labyrinth (gen/generate-labyrinth 100 100)
          all-walkable (get-all-walkable-tiles labyrinth)
          ;; Start flood fill from first room center
          first-room (first (:rooms labyrinth))
          start-x (+ (:x first-room) (quot (:width first-room) 2))
          start-y (+ (:y first-room) (quot (:height first-room) 2))
          connected-tiles (flood-fill [start-x start-y] all-walkable)]
      (is (= (count all-walkable) (count connected-tiles))
          (str "Not all tiles are connected! "
               "Total walkable: " (count all-walkable) ", "
               "Connected: " (count connected-tiles) ", "
               "Disconnected: " (- (count all-walkable) (count connected-tiles)))))))

(deftest test-all-rooms-reachable
  (testing "Every room is reachable from every other room"
    (let [labyrinth (gen/generate-labyrinth 100 100)
          all-walkable (get-all-walkable-tiles labyrinth)
          rooms (:rooms labyrinth)
          ;; Get center of first room
          first-room (first rooms)
          start-x (+ (:x first-room) (quot (:width first-room) 2))
          start-y (+ (:y first-room) (quot (:height first-room) 2))
          connected-tiles (flood-fill [start-x start-y] all-walkable)
          ;; Check if all room centers are reachable
          room-centers (map (fn [room]
                             [(+ (:x room) (quot (:width room) 2))
                              (+ (:y room) (quot (:height room) 2))])
                           rooms)
          unreachable (filter #(not (contains? connected-tiles %)) room-centers)]
      (is (empty? unreachable)
          (str "Some rooms are unreachable: " (count unreachable) " out of " (count rooms))))))

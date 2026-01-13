(ns stagebuilder.parser
  (:require [stage.environment :as env])
  (:gen-class))

(defn parse-ascii-map
  "Parse an ASCII text map file into a coordinate grid structure.
   Returns {:width w :height h :tiles {[x y] char}}"
  [text-content]
  (let [lines (clojure.string/split-lines text-content)
        height (count lines)
        width (if (empty? lines) 0 (apply max (map count lines)))
        tiles (into {}
                (for [y (range height)
                      x (range (count (nth lines y)))]
                  [[x y] (nth (nth lines y) x)]))]
    {:width width
     :height height
     :tiles tiles}))

(defn get-tile
  "Get the tile character at a specific coordinate."
  [grid x y]
  (get (:tiles grid) [x y] \space))

(defn find-walkable-tiles
  "Find all walkable (non-wall) tiles in the grid."
  [grid]
  (vec (filter (fn [[coord char]] (= char \space)) (:tiles grid))))

(defn find-walls
  "Find all wall tiles in the grid."
  [grid]
  (vec (filter (fn [[coord char]] (= char \#)) (:tiles grid))))

(defn grid->stage
  "Convert a parsed grid into a stage structure with coordgrids."
  [grid label]
  (let [walkable (into #{} (map first (find-walkable-tiles grid)))
        walls (into #{} (map first (find-walls grid)))]
    {:label label
     :width (:width grid)
     :height (:height grid)
     :walkable walkable
     :walls walls
     :grid grid}))

(defn load-stage-from-file
  "Load and parse a stage from an ASCII text file."
  [filename label]
  (let [content (slurp filename)
        grid (parse-ascii-map content)]
    (grid->stage grid label)))

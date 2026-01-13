(ns a-game.mapgen
  (:require [stagebuilder.generator :as gen])
  (:gen-class))

(defn -main
  "Generate a large playable labyrinth map."
  [& args]
  (let [filename (or (first args) "resources/maps/epic_labyrinth.txt")
        width (Integer/parseInt (or (second args) "8000"))
        height (Integer/parseInt (or (nth args 2 nil) "8000"))]
    (println (str "Generating " width "x" height " labyrinth..."))
    (let [start-time (System/currentTimeMillis)
          lab (gen/generate-labyrinth width height)
          gen-time (- (System/currentTimeMillis) start-time)]
      (println (str "Generated in " gen-time "ms"))
      (println (str "  - " (count (:rooms lab)) " rooms"))
      (println (str "  - " (count (:corridors lab)) " corridor tiles"))
      (println (str "Saving to " filename "..."))
      (let [save-start (System/currentTimeMillis)]
        (gen/save-labyrinth lab filename)
        (let [save-time (- (System/currentTimeMillis) save-start)]
          (println (str "Saved in " save-time "ms"))
          (println "Done!"))))))

(ns a-game.core
  (use [stage.environment :as stage])
  (use [actors.agent :as entity])
  (use [components.properties :as component])
  (:gen-class))



(defn update_worldstate
  "Progress the game forward by a turn. "
  [& arg]
  (println (format "AAAY! Sup, %s" arg)))

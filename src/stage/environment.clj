(ns stage.environment
  (:gen-class))


;; create a stage map and store geographical information
;; use premade map?  empty grid?  random map?
;; if using a premade map, needs parsing and storage?
(defn create_stage
  "Name a stage and define its map."
  ;;stage should have keyword arguments to produce overloaded type
  [label agent_pool mapfile &]
  (let [label {:label label}]
    ()
    )
  )

(defn build_tree "An ambiguous place where everything lives." [worldmap entity]
  (swap! worldmap conj  entity))
;; Not sure how to do this without atoms, but this makes enough sense for now.





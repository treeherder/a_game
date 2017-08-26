(ns stage.environment
  (use [actors.agent :as entity])
  (:gen-class))


;; create a stage map and store geographical information
;; use premade map?  empty grid?  random map?
;; if using a premade map, needs parsing and storage?
(defn create_stage
  "Name a stage and define its map."
  [label]
  (let [label {:label label}]
    ()
    )

  )
(defn stage
  "A data structure to hold all of the entities." [label]
  {:stageID "some UUID" :entities {:id "some UUID" :components "some properties"}})

(def agent_pool                                             ;; stage container for  agents
  (fn produce_agents [stage] #{})
  ;; poll a database of agents, return a map by identity of all available agents on a stage
  )


(defn map-function-on-map-vals [m f]
  (reduce (fn [altered-map [k v]] (assoc altered-map k (f v))) {} m))
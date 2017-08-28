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


(def agent_pool                                             ;; stage container for  agents
  (fn produce_agents [stage]
    (
      ()
      ;;do something to poll the worldmap datastructure and return all of the entities in it

      ))
  ;; poll a database of agents, return a map by identity of all available agents on a stage
  )
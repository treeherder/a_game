(ns actors.agent
  (:gen-class))

;; an entity is little more than its name:
(defn name_entity "Creates an entity ID." [] (str (java.util.UUID/randomUUID)))

;; What meaning it has aside from its name is entirely determined by its components
(defn _entity
  "A more effective, no-longer alternate case where everthing is a set, or a vector in a set."
  []
  {(name_entity) :components })

(defn agent_pool
  "The part of the stage object that holds all of the actors."
  [stage]                                            ;; stage container for  agents
  (let [stage (stage :agent_pool)]
    ())
  ;; poll a database of agents, return a map by identity of all available agents on a stage
  )
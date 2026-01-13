(ns actors.agent
  (:require [stage.environment :as stage])
  (:gen-class))

;; an entity is little more than its name:
(defn name_entity "Creates an entity ID." [] (str (java.util.UUID/randomUUID)))

;; What meaning it has aside from its name is entirely determined by its components
(defn _entity
  "A more effective, no-longer alternate case where everthing is a set, or a vector in a set."
  []
  {:id (name_entity) :components #{}})

(defn agent_pool
  "The part of the stage object that holds all of the actors."
  [stage]
  ;; Prefer using stage/get-agents which accepts both atoms and plain maps.
  (stage/get-agents stage))
  ;; poll a database of agents, return a map by identity of all available agents on a stage

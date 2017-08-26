(ns actors.agent
  (:gen-class)
  )



(defn name_entity  "Creates an entity ID." [] (str (java.util.UUID/randomUUID)))


(defn construct_agent
  "THIS COULD BE A PROBLEM"
  []
  ;;creates an agent
  (hash-map :id (name_entity) :components {}))
;;{:id UUID, :components {:NAME "some name" :HITPOINTS 50} }


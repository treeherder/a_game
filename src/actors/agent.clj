(ns actors.agent
  (:gen-class)
  )



(defn name_entity "Creates an entity ID." [] (str (java.util.UUID/randomUUID)))


(defn construct_agent
  "THIS COULD BE A PROBLEM"
  []
  ;;creates an agent
  (hash-map :id (name_entity) :components {}))
;;{:id UUID, :components {:NAME "some name" :HITPOINTS 50} }

(defn vectity
  "an alternate case where everthing is a set"
  [entity]
  [entity #{}]
  )



(defn gen_sum
  "generate some entities" [components]

  (reduce (fn [comps [e c]]
            (into comps {e c})) components
          [(vectity (name_entity))]))



(defn build_tree "A place where evrything lives." [worldmap entity]
  (swap! worldmap conj  entity))

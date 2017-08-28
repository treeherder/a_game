(ns actors.agent
  (:gen-class)
  )

(defn name_entity "Creates an entity ID." [] (str (java.util.UUID/randomUUID)))

(defn _agent
  "A more effective, no-longer alternate case where everthing is a set"
  [entity]
  [entity #{}]
  )

(defn build_tree "A place where everything lives." [worldmap entity]
  (swap! worldmap conj  entity))
;;not sure how to do this without atoms




(defn gen_som
  "generate some entities" [components]

  (reduce (fn [comps [e c]]
            (into comps {e c})) components
          [(_agent (name_entity))]))

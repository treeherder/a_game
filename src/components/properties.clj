(ns components.properties
  (:gen-class))

(defn add_component
  "Gives the entity a oomponent value and puts in the data structure.
  A perfect entity should be
  {:id UUID :components [{:component1 zzz} {:component22 etc}]"
  [entity component]
  (let [entity (transient entity)
        components (assoc! (transient (entity :components))
                           (component :label) (component :data))
        ]
    (persistent! (assoc! entity :components (persistent! components))))
  )



(defn set_component_configuration
  "Configure an entity's components according to component change parameters"
  [entity component_changes]
  )
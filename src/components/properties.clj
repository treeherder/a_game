(ns components.properties
  (:gen-class))

(defn create_entity
  "Gives the entity a component value and puts in the data structure."
  [& {:keys [entity, components]}]
  (let[ entity (transient entity)
        entity_id (entity :id)
 	entity_components (entity :components)
 	new_components (conj entity_components components)]
    (assoc! entity :components new_components)
    (persistent! entity)))
	 

(defn get_properties
  "Get properties of a given entity."
  [entity]
  (let[ entity (transient entity)
        entity_id (entity :id)
        entity_components (entity :components)]
    (prn entity_components))
  )

(defn set_component_configuration
  "Configure an entity's components according to component change parameters"
  [entity component_changes]
  
  (prn "ok")
)
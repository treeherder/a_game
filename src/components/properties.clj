(ns components.properties
  (:gen-class))

(defn create_entity
  "Gives the entity a component value and puts in the data structure."
  [& {:keys [entity, components]}]
  (let [tentity (transient entity)
    entity-components (:components entity)
    new-components (conj (or entity-components #{}) components)
    updated (assoc! tentity :components new-components)]
    (persistent! updated)))

(defn get_properties
  "Get properties of a given entity."
  [entity]
  (:components entity))

(defn set_component_configuration
  "Configure an entity's components according to component change parameters"
  [entity component_changes]
  (let [existing (:components entity)
    new-components (conj (or existing #{}) component_changes)]
    (assoc entity :components new-components)))
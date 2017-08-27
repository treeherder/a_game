(ns a-game.core
  (use [stage.environment :as stage])
  (use [actors.agent :as actor])
  (use [components.properties :as component])
  (:gen-class))



(defn contribute_entity_to_map
  "A data structure to hold all of the entities."
  ;;{:stageID "some UUID" :entities {:id "some UUID" :components "some properties"}}
  [mapfile ]
  (build_tree [(vectity (name_entity))]))
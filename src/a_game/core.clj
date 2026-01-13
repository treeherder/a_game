(ns a-game.core
  (use [stage.environment :as stage])
  (use [actors.agent :as actor])
  (use [components.properties :as component])
  (:gen-class))



(defn contribute_entity_to_map
  "A data structure to hold all of the entities."
  ;;{:stageID "some UUID" :entities {:id "some UUID" :components "some properties"}}
  [map_entity new_entity]
  (stage/add-entity map_entity new_entity)
  (stage/snapshot map_entity))

(defn _stagemap
  "A coordinate plane that is initalized by max (x,y,z) objects can occupy more than one space?
  and can certainly 'threaten' eachother from much farther, with cover taken into account.
  indiidual rooms must somehow reconcile between having Z space that is their own vs shared."
  [mapfile]
  ;; this function needs to take the mapfile and read it into a map structure
  (if (and mapfile (not (empty? (str mapfile))))
    (try
      {:mapfile mapfile :raw (slurp mapfile)}
      (catch Exception _
        {:mapfile mapfile :raw nil}))
    {}))


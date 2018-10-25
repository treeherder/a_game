(ns stage.environment_test
  (:require [clojure.test :refer :all]
            [stage.environment :refer :all]))

(deftest stage
  (testing "Is a datastructure that represents the map, the agents,
   and the non-agents entities.
   {:mapfile somefile :agent_pool {some set of agents} :item_pool {same deal} :stage_id id}" )
  (is (true? false )))

(deftest stage_map
  (testing "Accepts a [stage] and a parses the lists of entities and
   plots them on the map to be rendered:
   [[12 23 45] {some entity} ]" )

  (is (true? false )))
;; right now, this doesn't make a lot of sense outside of being a test function?

(defn test_blade
  "This object is made of mostly low-quality metal, and sharpened into a thin blade."
  []
  (components.properties/create_entity
	:entity (actors.agent/_entity)
  	:components {:hp 1, :hardness 5, :used_for ["handle", "kindling"] :reducible ["iron" ["smelting", "filing"]] }))

(defn scrap_wood
  "This object is made at least partially of low-quality wood. It is potentially flammable."
  [] ;;this could have optional keywords with default values for things like HP?
  (components.properties/create_entity
	:entity (actors.agent/_entity)
	:components {:hp 1, :hardness 5, :used_for ["handle", "kindling"] :inflamable true}))


(defn knife "A simple test object." [handle blade] 


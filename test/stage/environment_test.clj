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


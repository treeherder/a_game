(ns components.prop-test

  (:require [clojure.test :refer :all]))





(deftest add_entity
  (testing "Create an entity and associate the ID with the list of other entites.
    A critical step in building the stage datastructure."
    (is (= false (set? (contribute_entity_to_map [:z])))
        )))

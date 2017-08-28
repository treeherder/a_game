(ns a-game.core-test
  (:require [clojure.test :refer :all]
            [a-game.core :refer :all]
            ))

(def womap (atom #{}))

(deftest create_entity_list
  (testing "Create an entity and associate the ID with the list of components.
    Should belong to the stage data structure"
    (is (= true (set?
                   (a-game.core/contribute_entity_to_map womap (actors.agent/_agent))


                  ))
        )))

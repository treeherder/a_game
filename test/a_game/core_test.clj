(ns a-game.core-test
  (:require [clojure.test :refer :all]
            [a-game.core :refer :all]))

(deftest create_item
  (testing "Create an entity and associate the ID with the list of components.
    Should belong to the stage data structure"
    (is (= false (string?
                  (->> :identity
                       ((actors.agent/construct_agent [:some_prop :another :more :still] [:a_behavior :blah :baz])))))
        )))

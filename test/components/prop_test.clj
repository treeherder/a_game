 (ns components.prop-test
  (:require [clojure.test :refer :all]
            [components.properties :as props]
            [actors.agent :as agent]))

(deftest create-and-get-properties
  (let [e (agent/_entity)
        updated (props/create_entity :entity e :components {:hp 10 :name "rock"})]
    (is (some? (:components updated)))
    (is (contains? (:components updated) {:hp 10 :name "rock"}))
    (is (= (:components updated) (props/get_properties updated)))))

(deftest set-component-config
  (let [e (agent/_entity)
        updated (props/set_component_configuration e {:hp 5})]
    (is (contains? (:components updated) {:hp 5}))))
(ns components.prop-test

  (:require [clojure.test :refer :all]))



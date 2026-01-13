 (ns stage.environment_test
   (:require [clojure.test :refer :all]
             [stage.environment :refer :all]
     [actors.agent :refer :all]
     [components.properties :as props]))

(deftest stage
  (testing "create_stage returns an atom containing expected keys"
    (let [st (create_stage "test-stage" #{} #{} nil nil nil)]
      (is (instance? clojure.lang.IAtom st))
      (let [wm @st]
        (is (= "test-stage" (:label wm)))
        (is (map? wm))
        (is (contains? wm :agent_pool))
        (is (contains? wm :entities))
        (is (string? (:stage_id wm)))))))

(deftest stage_map
  (testing "Adding entities and agents updates the stage appropriately"
    (let [st (create_stage "m" #{} #{} nil nil nil)
          e (actors.agent/_entity)
          a (actors.agent/_entity)]
      (add-entity st e)
      (add-agent st a)
      (let [wm @st]
        (is (= e (get-in wm [:entities (:id e)])))
        (is (contains? (:agent_pool wm) a))))))
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


(defn knife
  "This object is made at least partially of low-quality wood. It is potentially flammable and has a sharp blade."
  [handle blade]
  (components.properties/create_entity
	:entity (actors.agent/_entity)
	:components {handle, blade}
	:reducible "to-components"  ))


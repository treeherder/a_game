 (ns stage.apply-event-test
  (:require [clojure.test :refer :all]
            [stage.environment :as env]
            [actors.agent :as agent]))

(deftest apply-event-add-entity
  (let [st (env/create_stage "t" #{} #{} nil nil nil)
        e (agent/_entity)
        _ (env/commit-event st {:type :add-entity :entity e})
        wm @st]
    (is (= e (get-in wm [:entities (:id e)])))))

(deftest apply-event-add-agent
  (let [st (env/create_stage "t2" #{} #{} nil nil nil)
        a (agent/_entity)
        _ (env/commit-event st {:type :add-agent :agent a})
        wm @st]
    (is (contains? (:agent_pool wm) a))))

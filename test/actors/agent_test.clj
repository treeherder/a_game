 (ns actors.agent-test
  (:require [clojure.test :refer :all]
            [actors.agent :as a]
            [stage.environment :as env]))

(deftest agent-pool-returns-seq
  (let [st (env/create_stage "s" #{} #{} nil nil nil)
        ag (a/_entity)]
    (env/add-agent st ag)
    (is (seq? (a/agent_pool st)))))

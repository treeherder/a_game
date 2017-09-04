(ns actors.agent
  (:require [clojure.test :refer :all]))


(deftest agent_pool
  (testing "Accepts a [stage] and returns a list? of acting agents (players and NPCS)" )
  (is (seq?
        (actors.agent/agent_pool "some stage")
        )))


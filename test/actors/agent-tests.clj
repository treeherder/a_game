(ns actors.agent
  (:require [clojure.test :refer :all]))


(deftest agent_pool
  (testing "Accepts a [stage] and returns a list? of acting agents (players and NPCS)" )
  (is (seq?
        (actors.agent/agent_pool "some stage")
        )))

;; any entity with pysicality should have options for a BODY keymap
;; many objects and agents will have bodies that are represented similarly to stages
;; each of these bodies has a set of unique, sometimes attached entities -- limbs, appendages
;; in some cases, these appendage entities might be groups of other, similar entities
;; arms are made of upper and lower segments, hands, shoulder, elbow, and wrist joints
;; hands are built from fingers, etc.
;; additonally, each of these entities hss constituent components and behaviors that need to
;; relayed up the top level of the data structure

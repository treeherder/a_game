(ns engine.input-test
  (:require [clojure.test :refer :all]
            [engine.input :as input]))

;; =============================================================================
;; Key to Action Mapping Tests
;; =============================================================================

(deftest wasd-key-mapping
  (testing "WASD keys map to move actions"
    (is (= :move-up (input/key->action \w)))
    (is (= :move-down (input/key->action \s)))
    (is (= :move-left (input/key->action \a)))
    (is (= :move-right (input/key->action \d)))))

(deftest uppercase-wasd
  (testing "Uppercase WASD also works"
    (is (= :move-up (input/key->action \W)))
    (is (= :move-down (input/key->action \S)))
    (is (= :move-left (input/key->action \A)))
    (is (= :move-right (input/key->action \D)))))

(deftest vim-key-mapping
  (testing "Vim hjkl keys map to move actions"
    (is (= :move-up (input/key->action \k)))
    (is (= :move-down (input/key->action \j)))
    (is (= :move-left (input/key->action \h)))
    (is (= :move-right (input/key->action \l)))))

(deftest uppercase-vim
  (testing "Uppercase vim keys also work"
    (is (= :move-up (input/key->action \K)))
    (is (= :move-down (input/key->action \J)))
    (is (= :move-left (input/key->action \H)))
    (is (= :move-right (input/key->action \L)))))

(deftest quit-key-mapping
  (testing "Q key maps to quit"
    (is (= :quit (input/key->action \q)))
    (is (= :quit (input/key->action \Q)))))

(deftest pause-key-mapping
  (testing "P key maps to pause"
    (is (= :pause (input/key->action \p)))
    (is (= :pause (input/key->action \P)))))

(deftest unknown-key-mapping
  (testing "Unknown keys return :unknown"
    (is (= :unknown (input/key->action \x)))
    (is (= :unknown (input/key->action \z)))
    (is (= :unknown (input/key->action \1)))))

;; =============================================================================
;; Action to Direction Tests
;; =============================================================================

(deftest action-to-direction
  (testing "Convert action to movement direction"
    (is (= :up (input/action->move :move-up)))
    (is (= :down (input/action->move :move-down)))
    (is (= :left (input/action->move :move-left)))
    (is (= :right (input/action->move :move-right)))
    (is (nil? (input/action->move :quit)))))

;; =============================================================================
;; Movement Action Detection Tests
;; =============================================================================

(deftest movement-action-detection
  (testing "Detect movement actions"
    (is (true? (input/movement-action? :move-up)))
    (is (true? (input/movement-action? :move-down)))
    (is (true? (input/movement-action? :move-left)))
    (is (true? (input/movement-action? :move-right)))
    (is (false? (input/movement-action? :quit)))
    (is (false? (input/movement-action? :pause)))
    (is (false? (input/movement-action? :unknown)))))

;; =============================================================================
;; Input Processing Tests
;; =============================================================================

(deftest process-movement-input
  (testing "Process movement input"
    (let [result (input/process-input \w)]
      (is (= :move-up (:action result)))
      (is (= :up (:direction result)))
      (is (true? (:movement? result))))))

(deftest process-quit-input
  (testing "Process quit input"
    (let [result (input/process-input \q)]
      (is (= :quit (:action result)))
      (is (nil? (:direction result)))
      (is (false? (:movement? result))))))

(deftest process-unknown-input
  (testing "Process unknown input"
    (let [result (input/process-input \x)]
      (is (= :unknown (:action result)))
      (is (nil? (:direction result)))
      (is (false? (:movement? result))))))

;; =============================================================================
;; Arrow Key Tests
;; =============================================================================

(deftest arrow-key-actions
  (testing "Arrow key symbols map to move actions"
    (is (= :move-up (input/key->action :up)))
    (is (= :move-down (input/key->action :down)))
    (is (= :move-left (input/key->action :left)))
    (is (= :move-right (input/key->action :right)))))

;; =============================================================================
;; Input Queue Tests
;; =============================================================================

(deftest create-input-queue
  (testing "Create an empty input queue"
    (let [queue (input/create-input-queue)]
      (is (some? queue))
      (is (empty? @queue)))))

(deftest queue-and-pop-input
  (testing "Queue and pop inputs"
    (let [queue (input/create-input-queue)]
      (input/queue-input! queue \w)
      (input/queue-input! queue \a)
      (is (= 2 (count @queue)))
      (is (= \w (input/pop-input! queue)))
      (is (= 1 (count @queue)))
      (is (= \a (input/pop-input! queue)))
      (is (empty? @queue)))))

(deftest pop-empty-queue
  (testing "Pop from empty queue returns nil"
    (let [queue (input/create-input-queue)]
      (is (nil? (input/pop-input! queue))))))

(deftest clear-input-queue
  (testing "Clear input queue"
    (let [queue (input/create-input-queue)]
      (input/queue-input! queue \w)
      (input/queue-input! queue \a)
      (input/clear-input-queue! queue)
      (is (empty? @queue)))))

;; =============================================================================
;; Number Pad Tests
;; =============================================================================

(deftest numpad-key-mapping
  (testing "Number pad keys map to move actions"
    (is (= :move-up (input/key->action \8)))
    (is (= :move-down (input/key->action \2)))
    (is (= :move-left (input/key->action \4)))
    (is (= :move-right (input/key->action \6)))))

;; =============================================================================
;; Space Key Tests
;; =============================================================================

(deftest space-key-mapping
  (testing "Space key maps to wait"
    (is (= :wait (input/key->action \space)))))

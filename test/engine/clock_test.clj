(ns engine.clock-test
  (:require [clojure.test :refer :all]
            [engine.clock :as clock]
            [stage.environment :as env]
            [actors.agent :as agent]
            [components.collision :as collision]))

;; =============================================================================
;; Clock Creation and Basic Tick Tests
;; =============================================================================

(deftest create-game-clock
  (testing "Create a new game clock with initial tick count of 0"
    (let [clk (clock/create-clock)]
      (is (map? clk))
      (is (= 0 (clock/current-tick clk)))
      (is (empty? (clock/pending-events clk))))))

(deftest advance-single-tick
  (testing "Advance the clock by one tick"
    (let [clk (clock/create-clock)
          advanced (clock/tick clk)]
      (is (= 1 (clock/current-tick advanced))))))

(deftest advance-multiple-ticks
  (testing "Advance the clock by multiple ticks"
    (let [clk (clock/create-clock)
          advanced (-> clk clock/tick clock/tick clock/tick)]
      (is (= 3 (clock/current-tick advanced))))))

(deftest advance-n-ticks
  (testing "Advance the clock by N ticks at once"
    (let [clk (clock/create-clock)
          advanced (clock/advance clk 10)]
      (is (= 10 (clock/current-tick advanced))))))

;; =============================================================================
;; Clock with Stage Integration
;; =============================================================================

(deftest clock-embedded-in-stage
  (testing "Stage can have an embedded clock"
    (let [stage (env/create_stage "timed-stage" #{} #{} nil nil nil)
          clk (clock/create-clock)]
      (clock/attach-to-stage! stage clk)
      (is (some? (clock/get-stage-clock @stage)))
      (is (= 0 (clock/current-tick (clock/get-stage-clock @stage)))))))

(deftest tick-stage-clock
  (testing "Advance the clock attached to a stage"
    (let [stage (env/create_stage "timed-stage" #{} #{} nil nil nil)
          clk (clock/create-clock)]
      (clock/attach-to-stage! stage clk)
      (clock/tick-stage! stage)
      (is (= 1 (clock/current-tick (clock/get-stage-clock @stage))))
      (clock/tick-stage! stage)
      (clock/tick-stage! stage)
      (is (= 3 (clock/current-tick (clock/get-stage-clock @stage)))))))

;; =============================================================================
;; Scheduled Events Tests
;; =============================================================================

(deftest schedule-future-event
  (testing "Schedule an event to fire at a future tick"
    (let [clk (clock/create-clock)
          event {:type :spawn-enemy :data {:x 10 :y 10}}
          scheduled (clock/schedule-event clk 5 event)]
      (is (= 1 (count (clock/pending-events scheduled))))
      (is (= 5 (:at-tick (first (clock/pending-events scheduled))))))))

(deftest schedule-multiple-events
  (testing "Schedule multiple events at different ticks"
    (let [clk (-> (clock/create-clock)
                  (clock/schedule-event 3 {:type :event-a})
                  (clock/schedule-event 5 {:type :event-b})
                  (clock/schedule-event 3 {:type :event-c}))]
      (is (= 3 (count (clock/pending-events clk))))
      ;; Events at tick 3
      (is (= 2 (count (clock/events-at-tick clk 3))))
      ;; Events at tick 5
      (is (= 1 (count (clock/events-at-tick clk 5)))))))

(deftest events-fire-at-correct-tick
  (testing "Events fire when their scheduled tick is reached"
    (let [fired-events (atom [])
          handler (fn [event] (swap! fired-events conj event))
          clk (-> (clock/create-clock)
                  (clock/schedule-event 2 {:type :test-event :value 42}))]
      ;; Tick 1 - no events yet
      (let [clk1 (clock/tick-and-process clk handler)]
        (is (= 1 (clock/current-tick clk1)))
        (is (empty? @fired-events))
        ;; Tick 2 - event should fire
        (let [clk2 (clock/tick-and-process clk1 handler)]
          (is (= 2 (clock/current-tick clk2)))
          (is (= 1 (count @fired-events)))
          (is (= 42 (:value (first @fired-events)))))))))

(deftest events-removed-after-firing
  (testing "Events are removed from pending after they fire"
    (let [clk (-> (clock/create-clock)
                  (clock/schedule-event 1 {:type :one-time-event}))
          clk-after (clock/tick-and-process clk (fn [_] nil))]
      (is (= 0 (count (clock/pending-events clk-after)))))))

(deftest schedule-relative-event
  (testing "Schedule an event relative to current tick"
    (let [clk (-> (clock/create-clock)
                  (clock/advance 10)
                  (clock/schedule-relative 5 {:type :delayed-event}))]
      ;; Event should be at tick 15 (10 + 5)
      (is (= 15 (:at-tick (first (clock/pending-events clk))))))))

;; =============================================================================
;; Tick Rate and Time Conversion
;; =============================================================================

(deftest default-tick-rate
  (testing "Default tick rate is set"
    (let [clk (clock/create-clock)]
      (is (pos? (clock/ticks-per-second clk))))))

(deftest custom-tick-rate
  (testing "Create clock with custom tick rate"
    (let [clk (clock/create-clock {:ticks-per-second 30})]
      (is (= 30 (clock/ticks-per-second clk))))))

(deftest convert-ticks-to-seconds
  (testing "Convert tick count to elapsed seconds"
    (let [clk (clock/create-clock {:ticks-per-second 20})
          clk-advanced (clock/advance clk 100)]
      ;; 100 ticks at 20 ticks/sec = 5 seconds
      (is (= 5.0 (clock/elapsed-seconds clk-advanced))))))

(deftest convert-seconds-to-ticks
  (testing "Convert seconds to tick count"
    (let [clk (clock/create-clock {:ticks-per-second 20})]
      ;; 3 seconds at 20 ticks/sec = 60 ticks
      (is (= 60 (clock/seconds-to-ticks clk 3))))))

;; =============================================================================
;; Pause/Resume Tests
;; =============================================================================

(deftest pause-clock
  (testing "Pause the clock"
    (let [clk (-> (clock/create-clock)
                  (clock/advance 5)
                  (clock/pause))]
      (is (clock/paused? clk))
      (is (= 5 (clock/current-tick clk))))))

(deftest resume-clock
  (testing "Resume a paused clock"
    (let [clk (-> (clock/create-clock)
                  (clock/pause)
                  (clock/resume))]
      (is (not (clock/paused? clk))))))

(deftest tick-while-paused-does-nothing
  (testing "Ticking a paused clock does not advance it"
    (let [clk (-> (clock/create-clock)
                  (clock/advance 5)
                  (clock/pause)
                  (clock/tick))]
      (is (= 5 (clock/current-tick clk))))))

;; =============================================================================
;; Entity Turn/Action Tests
;; =============================================================================

(deftest entity-action-cooldown
  (testing "Entity can have action cooldown in ticks"
    (let [player (-> (agent/_entity)
                     (clock/set-action-cooldown 3))]
      (is (= 3 (clock/get-action-cooldown player))))))

(deftest entity-ready-to-act
  (testing "Entity is ready to act when cooldown expires"
    (let [clk (clock/advance (clock/create-clock) 10)
          player (-> (agent/_entity)
                     (clock/set-last-action-tick 5)
                     (clock/set-action-cooldown 3))]
      ;; Last action at tick 5, cooldown 3 = ready at tick 8
      ;; Current tick is 10, so player is ready
      (is (clock/ready-to-act? player clk)))))

(deftest entity-not-ready-during-cooldown
  (testing "Entity cannot act during cooldown period"
    (let [clk (clock/advance (clock/create-clock) 6)
          player (-> (agent/_entity)
                     (clock/set-last-action-tick 5)
                     (clock/set-action-cooldown 3))]
      ;; Last action at tick 5, cooldown 3 = ready at tick 8
      ;; Current tick is 6, so player is NOT ready
      (is (not (clock/ready-to-act? player clk))))))

(deftest record-entity-action
  (testing "Record when an entity takes an action"
    (let [clk (clock/advance (clock/create-clock) 15)
          player (agent/_entity)
          after-action (clock/record-action player clk)]
      (is (= 15 (clock/get-last-action-tick after-action))))))

;; =============================================================================
;; Game Loop Integration Tests
;; =============================================================================

(deftest full-game-loop-tick
  (testing "Full game loop tick processes events and updates clock"
    (let [stage (env/create_stage "game-stage" #{} #{} nil nil nil)
          clk (clock/create-clock)
          processed (atom [])]
      ;; Attach clock and schedule events
      (clock/attach-to-stage! stage clk)
      (clock/schedule-stage-event! stage 1 {:type :game-start})
      (clock/schedule-stage-event! stage 3 {:type :spawn-wave :wave 1})
      
      ;; Run 3 ticks
      (doseq [_ (range 3)]
        (clock/tick-stage-with-handler! stage (fn [e] (swap! processed conj e))))
      
      (is (= 3 (clock/current-tick (clock/get-stage-clock @stage))))
      (is (= 2 (count @processed)))
      (is (some #(= :game-start (:type %)) @processed))
      (is (some #(= :spawn-wave (:type %)) @processed)))))

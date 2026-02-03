(ns engine.clock
  "Game clock system for tick-based game state updates.
   
   The clock tracks:
   - Current tick count (discrete time steps)
   - Scheduled events (fire at specific ticks)
   - Tick rate (ticks per second for time conversion)
   - Pause state
   
   Entities can have:
   - Action cooldowns (ticks until they can act again)
   - Last action tick (when they last performed an action)"
  (:gen-class))

;; =============================================================================
;; Clock Creation and Basic Operations
;; =============================================================================

(defn create-clock
  "Create a new game clock.
   Options:
     :ticks-per-second - how many ticks per real second (default 20)"
  ([] (create-clock {}))
  ([{:keys [ticks-per-second] :or {ticks-per-second 20}}]
   {:tick 0
    :ticks-per-second ticks-per-second
    :paused false
    :pending-events []}))

(defn current-tick
  "Get the current tick count."
  [clock]
  (:tick clock))

(defn ticks-per-second
  "Get the tick rate."
  [clock]
  (:ticks-per-second clock))

(defn paused?
  "Check if the clock is paused."
  [clock]
  (:paused clock))

(defn pending-events
  "Get all pending scheduled events."
  [clock]
  (:pending-events clock))

;; =============================================================================
;; Tick Advancement
;; =============================================================================

(defn tick
  "Advance the clock by one tick. Does nothing if paused."
  [clock]
  (if (paused? clock)
    clock
    (update clock :tick inc)))

(defn advance
  "Advance the clock by n ticks. Does nothing if paused."
  [clock n]
  (if (paused? clock)
    clock
    (update clock :tick + n)))

;; =============================================================================
;; Pause/Resume
;; =============================================================================

(defn pause
  "Pause the clock."
  [clock]
  (assoc clock :paused true))

(defn resume
  "Resume the clock."
  [clock]
  (assoc clock :paused false))

;; =============================================================================
;; Time Conversion
;; =============================================================================

(defn elapsed-seconds
  "Convert current tick count to elapsed seconds."
  [clock]
  (double (/ (current-tick clock) (ticks-per-second clock))))

(defn seconds-to-ticks
  "Convert seconds to tick count."
  [clock seconds]
  (* seconds (ticks-per-second clock)))

;; =============================================================================
;; Event Scheduling
;; =============================================================================

(defn schedule-event
  "Schedule an event to fire at a specific tick.
   The event map should have :type and any relevant data."
  [clock at-tick event]
  (update clock :pending-events conj {:at-tick at-tick :event event}))

(defn schedule-relative
  "Schedule an event relative to the current tick."
  [clock ticks-from-now event]
  (schedule-event clock (+ (current-tick clock) ticks-from-now) event))

(defn events-at-tick
  "Get all events scheduled for a specific tick."
  [clock tick-num]
  (filter #(= tick-num (:at-tick %)) (pending-events clock)))

(defn- remove-events-at-tick
  "Remove all events scheduled for a specific tick."
  [clock tick-num]
  (update clock :pending-events 
          (fn [events] (vec (remove #(= tick-num (:at-tick %)) events)))))

(defn tick-and-process
  "Advance the clock by one tick and process any scheduled events.
   Calls the handler function for each event that fires this tick.
   Returns the updated clock with fired events removed."
  [clock handler]
  (if (paused? clock)
    clock
    (let [new-clock (tick clock)
          current (current-tick new-clock)
          events-now (events-at-tick new-clock current)]
      ;; Fire all events for this tick
      (doseq [{:keys [event]} events-now]
        (handler event))
      ;; Remove fired events
      (remove-events-at-tick new-clock current))))

;; =============================================================================
;; Stage Integration
;; =============================================================================

(defn attach-to-stage!
  "Attach a clock to a stage (mutates the stage atom)."
  [stage-atom clock]
  (swap! stage-atom assoc :clock clock))

(defn get-stage-clock
  "Get the clock from a stage map."
  [stage]
  (:clock stage))

(defn tick-stage!
  "Advance the clock attached to a stage by one tick."
  [stage-atom]
  (swap! stage-atom update :clock tick))

(defn schedule-stage-event!
  "Schedule an event on the stage's clock."
  [stage-atom at-tick event]
  (swap! stage-atom update :clock #(schedule-event % at-tick event)))

(defn tick-stage-with-handler!
  "Advance the stage clock and process events with a handler."
  [stage-atom handler]
  (swap! stage-atom update :clock #(tick-and-process % handler)))

;; =============================================================================
;; Entity Action Timing
;; =============================================================================

(defn set-action-cooldown
  "Set how many ticks an entity must wait between actions."
  [entity cooldown-ticks]
  (assoc entity :action-cooldown cooldown-ticks))

(defn get-action-cooldown
  "Get an entity's action cooldown."
  [entity]
  (or (:action-cooldown entity) 0))

(defn set-last-action-tick
  "Set the tick when the entity last performed an action."
  [entity tick-num]
  (assoc entity :last-action-tick tick-num))

(defn get-last-action-tick
  "Get the tick when the entity last performed an action."
  [entity]
  (or (:last-action-tick entity) 0))

(defn ready-to-act?
  "Check if an entity is ready to perform an action.
   Returns true if current tick >= last action tick + cooldown."
  [entity clock]
  (let [last-action (get-last-action-tick entity)
        cooldown (get-action-cooldown entity)
        ready-tick (+ last-action cooldown)]
    (>= (current-tick clock) ready-tick)))

(defn record-action
  "Record that an entity just performed an action at the current tick."
  [entity clock]
  (set-last-action-tick entity (current-tick clock)))

(defn ticks-until-ready
  "Calculate how many ticks until an entity can act again."
  [entity clock]
  (let [last-action (get-last-action-tick entity)
        cooldown (get-action-cooldown entity)
        ready-tick (+ last-action cooldown)
        current (current-tick clock)]
    (max 0 (- ready-tick current))))

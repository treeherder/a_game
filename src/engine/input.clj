(ns engine.input
  "Input handling for player controls.
   
   Maps keyboard input to game actions:
   - Arrow keys / WASD / hjkl for movement
   - q to quit
   - p to pause"
  (:gen-class))

;; =============================================================================
;; Input Mapping
;; =============================================================================

(def key-bindings
  "Default key bindings for movement and actions."
  {;; Arrow keys (represented as keywords)
   :up    :move-up
   :down  :move-down
   :left  :move-left
   :right :move-right
   ;; WASD
   \w :move-up
   \W :move-up
   \s :move-down
   \S :move-down
   \a :move-left
   \A :move-left
   \d :move-right
   \D :move-right
   ;; Vi-style
   \h :move-left
   \j :move-down
   \k :move-up
   \l :move-right
   \H :move-left
   \J :move-down
   \K :move-up
   \L :move-right
   ;; Actions
   \q :quit
   \Q :quit
   \p :pause
   \P :pause
   \space :wait
   ;; Number pad (as characters)
   \8 :move-up
   \2 :move-down
   \4 :move-left
   \6 :move-right})

(def action->direction
  "Map actions to movement directions."
  {:move-up    :up
   :move-down  :down
   :move-left  :left
   :move-right :right})

;; =============================================================================
;; Input Processing
;; =============================================================================

(defn key->action
  "Convert a key input to a game action."
  [key]
  (get key-bindings key :unknown))

(defn action->move
  "Convert an action to a movement direction, or nil if not a movement."
  [action]
  (get action->direction action))

(defn movement-action?
  "Check if an action is a movement action."
  [action]
  (contains? action->direction action))

(defn process-input
  "Process a key input and return an action map.
   Returns {:action :action-type :direction :dir-or-nil}"
  [key]
  (let [action (key->action key)]
    {:action action
     :direction (action->move action)
     :movement? (movement-action? action)}))

;; =============================================================================
;; Input Queue (for buffered input)
;; =============================================================================

(defn create-input-queue
  "Create an input queue for buffering player input."
  []
  (atom []))

(defn queue-input!
  "Add an input to the queue."
  [queue key]
  (swap! queue conj key))

(defn pop-input!
  "Pop the next input from the queue. Returns nil if empty."
  [queue]
  (let [inputs @queue]
    (when (seq inputs)
      (swap! queue #(vec (rest %)))
      (first inputs))))

(defn clear-input-queue!
  "Clear all pending inputs."
  [queue]
  (reset! queue []))

;; =============================================================================
;; Console Input (blocking)
;; =============================================================================

(defn read-char
  "Read a single character from stdin (blocking)."
  []
  (let [reader (java.io.BufferedReader. *in*)]
    (char (.read reader))))

(defn read-line-input
  "Read a line of input (blocking). Returns the first character."
  []
  (first (read-line)))

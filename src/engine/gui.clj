(ns engine.gui
  "Swing-based GUI window for real-time gameplay."
  (:require [engine.game :as game]
            [engine.clock :as clock]
            [engine.input :as input]
            [components.collision :as collision])
  (:import [javax.swing JFrame JPanel Timer]
           [java.awt Color Font Graphics Dimension]
           [java.awt.event KeyAdapter KeyEvent ActionListener])
  (:gen-class))

;; =============================================================================
;; Configuration
;; =============================================================================

(def ^:const tile-size 16)
(def ^:const font-size 14)

;; =============================================================================
;; Key Handling
;; =============================================================================

(defn keycode->char
  "Convert Java KeyEvent keycode to character or keyword."
  [keycode]
  (case keycode
    87 \w      ; W
    65 \a      ; A
    83 \s      ; S
    68 \d      ; D
    38 :up     ; UP arrow
    40 :down   ; DOWN arrow
    37 :left   ; LEFT arrow
    39 :right  ; RIGHT arrow
    72 \h      ; H
    74 \j      ; J
    75 \k      ; K
    76 \l      ; L
    32 \space  ; SPACE
    81 \q      ; Q
    103 \7     ; NUMPAD7
    105 \9     ; NUMPAD9
    97 \1      ; NUMPAD1
    99 \3      ; NUMPAD3
    nil))

(defn create-key-listener
  "Create a KeyAdapter that tracks pressed keys."
  [pressed-keys-atom]
  (proxy [KeyAdapter] []
    (keyPressed [^KeyEvent e]
      (let [keycode (.getKeyCode e)
            k (keycode->char keycode)]
        (when k
          (swap! pressed-keys-atom conj k))))
    (keyReleased [^KeyEvent e]
      (when-let [k (keycode->char (.getKeyCode e))]
        (swap! pressed-keys-atom disj k)))))

;; =============================================================================
;; Rendering
;; =============================================================================

(defn render-tile
  "Render a single tile character."
  [g x y c]
  (let [px (* x tile-size)
        py (* y tile-size)]
    (case c
      \# (do (.setColor g Color/GRAY)
             (.fillRect g px py tile-size tile-size)
             (.setColor g Color/DARK_GRAY)
             (.drawRect g px py tile-size tile-size))
      \. (do (.setColor g (Color. 50 50 50))
             (.fillRect g px py tile-size tile-size))
      \@ (do (.setColor g Color/YELLOW)
             (.fillOval g (+ px 2) (+ py 2) (- tile-size 4) (- tile-size 4)))
      ;; Default - draw character
      (do (.setColor g Color/WHITE)
          (.drawString g (str c) px (+ py tile-size -2))))))

(defn draw-game-state
  "Draw the entire game state to graphics context."
  [g game-state pressed-keys-atom width height]
  ;; Background
  (.setColor g Color/BLACK)
  (.fillRect g 0 0 (* width tile-size) (* height tile-size))
  
  ;; Render game
  (let [render-str (game/render-game-state game-state)
        lines (clojure.string/split-lines render-str)]
    (doseq [[y line] (map-indexed vector lines)
            [x c] (map-indexed vector line)]
      (render-tile g x y c)))
  
  ;; UI overlay
  (let [stage-atom (:stage @game-state)
        player-id (:player-id @game-state)
        stage (when stage-atom @stage-atom)
        player (when (and stage player-id)
                 (first (filter #(= player-id (:id %)) (:agent_pool stage))))
        coll (when player (collision/get-collision player))
        pos (when coll [(:x coll) (:y coll)])
        tick (clock/current-tick (:clock @game-state))]
    (.setColor g Color/WHITE)
    (.setFont g (Font. "Monospaced" Font/PLAIN 12))
    (.drawString g (str "Pos: " pos " | Tick: " tick)
                 10 (+ (* height tile-size) 20))))

;; =============================================================================
;; Game Panel
;; =============================================================================

(defn create-game-panel
  "Create the main game panel."
  [game-state pressed-keys-atom width height]
  (let [panel (proxy [JPanel] []
                (paintComponent [g]
                  (proxy-super paintComponent g)
                  (draw-game-state g game-state pressed-keys-atom width height)))]
    (.setPreferredSize panel (Dimension. (* width tile-size) 
                                         (+ (* height tile-size) 40)))
    (.setBackground panel Color/BLACK)
    (.setFocusable panel true)
    (.addKeyListener panel (create-key-listener pressed-keys-atom))
    panel))

;; =============================================================================
;; Game Loop
;; =============================================================================

(defn process-keys!
  "Process all currently pressed keys with cooldown."
  [game-state pressed-keys-atom last-move-time]
  (when-let [k (first @pressed-keys-atom)]
    (let [current-time (System/currentTimeMillis)
          time-since-move (- current-time @last-move-time)
          move-cooldown 150]  ; 150ms between moves (about 6-7 moves per second)
      (when (>= time-since-move move-cooldown)
        (if (= k \q)
          (swap! game-state assoc :running false)
          (do
            (game/process-player-input! game-state k)
            (reset! last-move-time current-time)))))))

(defn create-game-timer
  "Create a timer for the game loop (60 FPS)."
  [panel game-state pressed-keys-atom]
  (let [last-move-time (atom 0)
        timer (Timer. 16  ; ~60 FPS
                      (reify ActionListener
                        (actionPerformed [_ _]
                          (when (:running @game-state)
                            (process-keys! game-state pressed-keys-atom last-move-time)
                            (.repaint panel)))))]
    (.start timer)
    timer))

;; =============================================================================
;; Window Creation
;; =============================================================================

(defn create-window
  "Create and display the game window."
  ([game-state] (create-window game-state 50 30))
  ([game-state width height]
   (let [pressed-keys-atom (atom #{})
         frame (JFrame. "A Game - Mazelike")
         panel (create-game-panel game-state pressed-keys-atom width height)
         timer (create-game-timer panel game-state pressed-keys-atom)]
     (.add frame panel)
     (.pack frame)
     (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
     (.setLocationRelativeTo frame nil)
     (.setVisible frame true)
     ;; Ensure panel has focus after window is visible
     (javax.swing.SwingUtilities/invokeLater
      #(.requestFocusInWindow panel))
     {:frame frame
      :panel panel
      :timer timer
      :pressed-keys pressed-keys-atom})))

(defn start-gui!
  "Start the GUI version of the game."
  ([] (start-gui! 50 30))
  ([width height]
   (let [game-state (game/game-state-atom)]
     (println "Generating labyrinth...")
     (game/setup-stage-with-labyrinth game-state width height)
     (println "Starting GUI...")
     (javax.swing.SwingUtilities/invokeLater
      #(create-window game-state width height))
     game-state)))

(defn -main
  "Main entry point for GUI version."
  [& args]
  (let [width (if (first args) (Integer/parseInt (first args)) 50)
        height (if (second args) (Integer/parseInt (second args)) 30)]
    (start-gui! width height)
    ;; Keep main thread alive
    (while true (Thread/sleep 1000))))

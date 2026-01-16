(ns stagebuilder.generator
  "Procedural labyrinth generator using Wilson's algorithm with room-based cells.
   
   Generated maps use monospace-compatible ASCII characters:
   - '#' for walls
   - '.' (period) for walkable floor/corridor tiles
   
   Algorithm:
   1. Subdivide map into grid of cells
   2. Place rooms in ~50% of cells (random sizes)
   3. Use Wilson's algorithm to generate unbiased maze connecting rooms
   4. Add 1-3 border exits
   
   To view generated maps correctly, use a monospace font (e.g., Courier, 
   Consolas, Monaco, or any code editor default font). Each character 
   represents one tile with equal width and height dimensions."
  (:gen-class))

(defn generate-room
  "Generate a rectangular room with walls and floor."
  [x y width height]
  {:x x :y y :width width :height height
   :type :room})

(defn subdivide-into-cells
  "Subdivide map into grid cells. Returns vector of cell bounds."
  [width height cell-size]
  (let [cols (quot width cell-size)
        rows (quot height cell-size)]
    (for [row (range rows)
          col (range cols)]
      {:col col :row row
       :x (* col cell-size)
       :y (* row cell-size)
       :width cell-size
       :height cell-size})))

(defn place-room-in-cell
  "Place a randomly sized room within a cell with padding."
  [cell]
  (let [padding 3
        min-room-size 4
        max-width (max min-room-size (- (:width cell) (* 2 padding)))
        max-height (max min-room-size (- (:height cell) (* 2 padding)))
        ;; Random room size (small, medium, large)
        size-factor (rand-nth [0.3 0.5 0.7 0.9])
        room-w (max min-room-size (int (* max-width size-factor)))
        room-h (max min-room-size (int (* max-height size-factor)))
        ;; Center room in cell with some random offset
        offset-x (rand-int (max 1 (- max-width room-w)))
        offset-y (rand-int (max 1 (- max-height room-h)))
        room-x (+ (:x cell) padding offset-x)
        room-y (+ (:y cell) padding offset-y)]
    (generate-room room-x room-y room-w room-h)))

(defn generate-rooms-in-cells
  "Place rooms in approximately 50% of cells."
  [cells]
  (let [num-rooms (max 1 (int (* (count cells) 0.5)))]
    (->> cells
         shuffle
         (take num-rooms)
         (map place-room-in-cell)
         vec)))

(defn point-in-room?
  "Check if a point (x,y) is inside a room (walls or floor)."
  [x y room]
  (and (>= x (:x room))
       (< x (+ (:x room) (:width room)))
       (>= y (:y room))
       (< y (+ (:y room) (:height room)))))

(defn point-in-any-room?
  "Check if a point is inside any room."
  [x y rooms]
  (some #(point-in-room? x y %) rooms))

(defn carvable?
  "Check if position can be carved (odd coordinates, in bounds, not in room)."
  [x y width height rooms]
  (and (odd? x) (odd? y)
       (>= x 1) (< x (dec width))
       (>= y 1) (< y (dec height))
       (not (point-in-any-room? x y rooms))))

(defn get-carvable-neighbors
  "Get uncarved neighbors 2 cells away."
  [x y width height rooms carved]
  (let [directions [[-2 0] [2 0] [0 -2] [0 2]]]
    (filter (fn [[dx dy]]
              (let [nx (+ x dx)
                    ny (+ y dy)]
                (and (carvable? nx ny width height rooms)
                     (not (contains? carved [nx ny])))))
            directions)))

(defn carve-maze-recursive
  "Carve maze using recursive backtracking algorithm."
  [start-x start-y width height rooms max-cells]
  (loop [stack [[start-x start-y]]
         carved #{[start-x start-y]}
         paths []]
    (if (or (empty? stack) (>= (count carved) max-cells))
      paths
      (let [[x y] (peek stack)
            neighbors (get-carvable-neighbors x y width height rooms carved)]
        (if (empty? neighbors)
          (recur (pop stack) carved paths)
          (let [[dx dy] (rand-nth neighbors)
                nx (+ x dx)
                ny (+ y dy)
                ;; Add both the new cell and the wall between
                mid-x (+ x (quot dx 2))
                mid-y (+ y (quot dy 2))
                new-paths (concat paths [[x y] [mid-x mid-y] [nx ny]])]
            (recur (conj stack [nx ny])
                   (conj carved [nx ny])
                   new-paths)))))))

(defn carve-maze-in-empty-space
  "Carve maze throughout empty space not occupied by rooms."
  [width height rooms room-corridors]
  (let [;; Find all valid starting points adjacent to existing corridors
        corridor-set (set room-corridors)
        ;; Find odd coordinates adjacent to room corridors
        starts-near-corridors (set (for [[cx cy] corridor-set
                                         [dx dy] [[-1 0] [1 0] [0 -1] [0 1]]
                                         :let [nx (+ cx dx)
                                               ny (+ cy dy)]
                                         :when (and (carvable? nx ny width height rooms)
                                                   (not (contains? corridor-set [nx ny])))]
                                     [nx ny]))
        ;; Also include other odd coordinates for backup
        all-potential-starts (for [y (range 1 height 2)
                                  x (range 1 width 2)
                                  :when (carvable? x y width height rooms)]
                              [x y])
        ;; Prioritize starts near corridors, then random starts
        potential-starts (concat (shuffle (vec starts-near-corridors))
                                (shuffle all-potential-starts))
        max-cells-per-section (quot (* width height) 100)]
    (loop [remaining-starts (distinct potential-starts)
           all-paths []]
      (if (empty? remaining-starts)
        (set all-paths)
        (let [start (first remaining-starts)
              [sx sy] start
              paths (carve-maze-recursive sx sy width height rooms max-cells-per-section)
              carved-coords (set (map (fn [[x y]] [x y]) paths))
              new-remaining (remove (fn [coord] (contains? carved-coords coord)) remaining-starts)]
          (recur new-remaining (concat all-paths paths)))))))

;;; ============================================================
;;; Dead End Generation - Creates winding, deceptive corridors
;;; ============================================================

(defn get-carve-neighbors
  "Get valid adjacent positions for dead-end carving (2 cells away, odd coords)."
  [x y width height rooms existing-paths]
  (let [directions [[-2 0] [2 0] [0 -2] [0 2]]]
    (->> directions
         (map (fn [[dx dy]]
                (let [nx (+ x dx)
                      ny (+ y dy)
                      mid-x (+ x (quot dx 2))
                      mid-y (+ y (quot dy 2))]
                  (when (and (carvable? nx ny width height rooms)
                             (not (contains? existing-paths [nx ny]))
                             (not (contains? existing-paths [mid-x mid-y])))
                    {:dir [dx dy] :pos [nx ny] :mid [mid-x mid-y]}))))
         (filter some?))))

(defn carve-winding-dead-end
  "Carve a single winding dead-end branch from a starting point.
   Uses curve-bias to create organic, winding paths that deceive the player."
  [start-x start-y width height rooms existing-paths target-length]
  (loop [x start-x
         y start-y
         carved #{[start-x start-y]}
         last-dir nil
         steps 0
         ;; Curve bias: tendency to curve left, right, or go straight
         curve-bias (rand-nth [:left :right :straight :straight])]
    (if (>= steps target-length)
      carved
      (let [all-paths (into existing-paths carved)
            neighbors (get-carve-neighbors x y width height rooms all-paths)]
        (if (empty? neighbors)
          carved  ;; Natural dead end
          (let [;; Choose direction based on curve bias and last direction
                chosen (if (and last-dir (> (clojure.core/count neighbors) 1))
                         (let [[ldx ldy] last-dir
                               ;; Classify neighbors relative to last direction
                               same (filter #(= (:dir %) last-dir) neighbors)
                               perp (filter #(let [[dx dy] (:dir %)]
                                               (or (and (zero? ldx) (zero? dy))
                                                   (and (zero? ldy) (zero? dx))))
                                            neighbors)]
                           (case curve-bias
                             :straight (or (first same) (rand-nth neighbors))
                             :left (or (first perp) (first same) (rand-nth neighbors))
                             :right (or (last perp) (first same) (rand-nth neighbors))
                             (rand-nth neighbors)))
                         (rand-nth neighbors))
                {:keys [dir pos mid]} chosen
                [nx ny] pos
                [mx my] mid
                ;; Occasionally change curve bias for natural winding
                new-bias (if (< (rand) 0.2)
                           (rand-nth [:left :right :straight :straight])
                           curve-bias)]
            (recur nx ny
                   (conj carved pos mid)
                   dir
                   (inc steps)
                   new-bias)))))))

(defn find-branch-points
  "Find good attachment points for dead ends on existing corridors.
   Returns points that have 1-2 adjacent corridor neighbors."
  [corridors width height rooms]
  (let [corridor-set (set corridors)]
    (->> corridors
         (filter (fn [[x y]]
                   (let [adj-count (->> [[(dec x) y] [(inc x) y] [x (dec y)] [x (inc y)]]
                                        (filter #(contains? corridor-set %))
                                        clojure.core/count)]
                     (and (<= 1 adj-count 2)
                          (carvable? x y width height rooms)))))
         shuffle)))

(defn generate-dead-ends
  "Generate multiple winding dead-end branches off existing corridors."
  [width height rooms existing-corridors num-dead-ends]
  (let [branch-points (find-branch-points existing-corridors width height rooms)]
    (loop [points (take (* 3 num-dead-ends) branch-points)  ;; Try extra points
           all-carved #{}
           created 0]
      (if (or (empty? points) (>= created num-dead-ends))
        all-carved
        (let [[px py] (first points)
              ;; Variable length: some short (3-6), some long (10-20)
              target-len (if (< (rand) 0.3)
                           (+ 10 (rand-int 12))   ;; Long winding dead end
                           (+ 3 (rand-int 5)))    ;; Short dead end
              branch (carve-winding-dead-end px py width height rooms
                                              (into existing-corridors all-carved)
                                              target-len)
              branch-size (clojure.core/count branch)]
          (if (> branch-size 2)  ;; Only count substantial branches
            (recur (rest points) (into all-carved branch) (inc created))
            (recur (rest points) all-carved created)))))))

(defn add-secondary-branches
  "Add smaller dead-end branches off primary dead ends for extra deception."
  [width height rooms base-corridors primary-dead-ends]
  (let [combined (into base-corridors primary-dead-ends)
        branch-points (find-branch-points primary-dead-ends width height rooms)
        num-secondary (max 1 (quot (clojure.core/count primary-dead-ends) 20))]
    (loop [points (take (* 2 num-secondary) branch-points)
           all-carved #{}
           created 0]
      (if (or (empty? points) (>= created num-secondary))
        all-carved
        (let [[px py] (first points)
              target-len (+ 2 (rand-int 4))  ;; Short secondary branches
              branch (carve-winding-dead-end px py width height rooms
                                              (into combined all-carved)
                                              target-len)]
          (recur (rest points)
                 (into all-carved branch)
                 (inc created)))))))

;;; ============================================================

(defn get-room-connections
  "Get direct L-shaped corridor connecting two room centers."
  [room1 room2]
  (let [[x1 y1] [(+ (:x room1) (quot (:width room1) 2))
                 (+ (:y room1) (quot (:height room1) 2))]
        [x2 y2] [(+ (:x room2) (quot (:width room2) 2))
                 (+ (:y room2) (quot (:height room2) 2))]]
    (concat
      ;; Horizontal segment
      (for [x (range (min x1 x2) (inc (max x1 x2)))] [x y1])
      ;; Vertical segment
      (for [y (range (min y1 y2) (inc (max y1 y2)))] [x2 y]))))

(defn connect-all-rooms
  "Connect all rooms in a minimum spanning tree fashion."
  [rooms]
  (if (empty? rooms)
    #{}
    (loop [connected [(first rooms)]
           unconnected (rest rooms)
           corridors #{}]
      (if (empty? unconnected)
        corridors
        (let [;; Find closest unconnected room to any connected room
              best-pair (first (sort-by
                                (fn [[c u]]
                                  (let [cx (+ (:x c) (quot (:width c) 2))
                                        cy (+ (:y c) (quot (:height c) 2))
                                        ux (+ (:x u) (quot (:width u) 2))
                                        uy (+ (:y u) (quot (:height u) 2))]
                                    (+ (Math/abs (- ux cx)) (Math/abs (- uy cy)))))
                                (for [c connected u unconnected] [c u])))
              [conn-room unconn-room] best-pair
              new-corridor (get-room-connections conn-room unconn-room)]
          (recur (conj connected unconn-room)
                 (remove #(= % unconn-room) unconnected)
                 (into corridors new-corridor)))))))

(defn add-border-exits
  "Add 1-3 random exits on the map border."
  [width height rooms existing-corridors]
  (let [num-exits (+ 1 (rand-int 3))
        borders [:top :bottom :left :right]
        chosen-borders (take num-exits (shuffle borders))]
    (mapcat (fn [border]
              (let [[x y] (case border
                            :top [(+ 5 (rand-int (- width 10))) 0]
                            :bottom [(+ 5 (rand-int (- width 10))) (dec height)]
                            :left [0 (+ 5 (rand-int (- height 10)))]
                            :right [(dec width) (+ 5 (rand-int (- height 10)))])
                    ;; Create a short path inward
                    depth 3
                    path (case border
                          :top (for [dy (range depth)] [x dy])
                          :bottom (for [dy (range depth)] [x (- (dec height) dy)])
                          :left (for [dx (range depth)] [dx y])
                          :right (for [dx (range depth)] [(- (dec width) dx) y]))]
                path))
            chosen-borders)))

(defn generate-labyrinth
  "Generate an ASCII labyrinth using grid-cell approach with rooms and connecting corridors.
   Includes winding dead-ends that create deceptive paths to confuse players."
  ([width height] (generate-labyrinth width height 20))
  ([width height cell-size]
   (let [;; Subdivide into cells and place rooms
         cells (subdivide-into-cells width height cell-size)
         rooms (generate-rooms-in-cells cells)
         ;; Connect all rooms with corridors
         room-corridors (connect-all-rooms rooms)
         ;; Carve extensive maze in empty space (starting from room corridors)
         maze-paths (carve-maze-in-empty-space width height rooms room-corridors)
         ;; Combine base corridors
         base-corridors (into room-corridors maze-paths)
         
         ;; Generate winding dead ends (about 15% of corridor count, min 5)
         num-dead-ends (max 5 (quot (clojure.core/count maze-paths) 7))
         primary-dead-ends (generate-dead-ends width height rooms base-corridors num-dead-ends)
         
         ;; Add secondary branches off dead ends for extra deception
         secondary-dead-ends (add-secondary-branches width height rooms 
                                                      base-corridors primary-dead-ends)
         
         ;; Combine all corridor types
         corridors (-> base-corridors
                       (into primary-dead-ends)
                       (into secondary-dead-ends))
         ;; Add border exits
         exits (add-border-exits width height rooms corridors)
         ;; Combine all walkable areas
         all-corridors (into corridors exits)]
     {:width width
      :height height
      :rooms rooms
      :corridors (set all-corridors)})))

(defn point-in-room?
  "Check if a point (x,y) is inside a room (walls or floor)."
  [x y room]
  (and (>= x (:x room))
       (< x (+ (:x room) (:width room)))
       (>= y (:y room))
       (< y (+ (:y room) (:height room)))))

(defn point-on-room-wall?
  "Check if a point is on a room's wall."
  [x y room]
  (and (point-in-room? x y room)
       (or (= x (:x room))
           (= x (dec (+ (:x room) (:width room))))
           (= y (:y room))
           (= y (dec (+ (:y room) (:height room)))))))

(defn get-tile-at
  "Get the ASCII character for a given coordinate in the labyrinth."
  [x y labyrinth]
  (let [rooms (:rooms labyrinth)
        corridors (or (:corridors labyrinth) #{})]
    (cond
      ;; Check if in a corridor
      (contains? corridors [x y]) \.
      ;; Check if inside a room (floor or wall)
      (some #(point-in-room? x y %) rooms)
      (if (some #(point-on-room-wall? x y %) rooms)
        \#  ;; Wall
        \.) ;; Floor
      ;; Otherwise corridor/wall
      :else \#)))

(defn render-labyrinth-line
  "Render a single line of the labyrinth.
   Each character represents a single tile. Output is monospace-compatible."
  [y labyrinth]
  (apply str (map #(get-tile-at % y labyrinth) (range (:width labyrinth)))))

(defn render-labyrinth
  "Render the entire labyrinth as a string."
  [labyrinth]
  (clojure.string/join "\n"
    (map #(render-labyrinth-line % labyrinth) (range (:height labyrinth)))))

(defn save-labyrinth
  "Save a labyrinth to a file.
   
   Output format: Plain ASCII text, one character per tile.
   View with a monospace font for correct tile alignment.
   - '#' = wall
   - '.' = floor/corridor (walkable space)"
  [labyrinth filename]
  (spit filename (render-labyrinth labyrinth)))

(defn generate-full-labyrinth
  "Generate a complete 8k x 8k labyrinth ready for gameplay."
  []
  (generate-labyrinth 8000 8000))

(defn save-full-labyrinth
  "Generate and save a full 8k x 8k labyrinth to a file."
  [filename]
  (let [lab (generate-full-labyrinth)]
    (save-labyrinth lab filename)
    lab))

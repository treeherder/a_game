 (ns stage.environment
  (:require [clojure.data.json :as json])
  (:gen-class))


;; create a stage map and store geographical information
;; use premade map?  empty grid?  random map?
;; if using a premade map, needs parsing and storage?

(defn create_stage
  "Name a stage and define its map."
  ;;stage should have keyword arguments to produce overloaded type
  [label agent_pool item_pool mapfile coordgrids rules & _]
  (let [stage {:label label
               :agent_pool (or agent_pool #{})
               :item_pool (or item_pool #{})
               :mapfile mapfile
               :coordgrids coordgrids
               :rules rules
               :entities {}
               :stage_id (str (java.util.UUID/randomUUID))}]
    (atom stage)))

(defn build_tree
  "An ambiguous place where everything lives. Accepts an atom that
   may contain either a set (world entity set) or a stage map."
  [worldmap entity]
  (swap! worldmap (fn [wm]
                    (cond
                      (set? wm) (conj wm entity)
                      (map? wm) (assoc wm :entities (assoc (get wm :entities {}) (:id entity) entity))
                      :else (conj (or wm #{}) entity)))))

(defn add-entity
  "Add an entity (map with :id) into the world's :entities and return the entity.
   `world` may be an atom or a plain map; if atom, it will be updated." 
  [world entity]
  (if (instance? clojure.lang.IAtom world)
    (let [wm @world]
      (if (set? wm)
        (do (swap! world conj entity) entity)
        (do (swap! world update :entities (fn [m] (assoc (or m {}) (:id entity) entity))) entity)))
    (assoc-in world [:entities (:id entity)] entity)))

(defn add-agent
  "Add an agent into the world's :agent_pool. Accepts atom or plain map." 
  [world agent]
  (if (instance? clojure.lang.IAtom world)
    (do (swap! world update :agent_pool (fn [s] (conj (or s #{}) agent)))
        agent)
    (update world :agent_pool (fn [s] (conj (or s #{}) agent)))))

(defn get-agents
  "Return a seq of agents from a world atom or map." 
  [world]
  (let [wm (if (instance? clojure.lang.IAtom world) @world world)]
    (seq (:agent_pool wm))))

(defn snapshot
  "Return a snapshot (plain map) of the world (deref if atom)." [world]
  (if (instance? clojure.lang.IAtom world) @world world))

(defn event->json
  "Serialize an event map to JSON string. Keys will become strings." [event]
  (json/write-str event))

(defn json->event
  "Deserialize JSON string into an event map. Keys are converted to keywords." [s]
  (json/read-str s :key-fn keyword))

(defn apply-event
  "Pure function: apply an event map to a world (plain map) and return new world.
   Supported events: {:type :add-entity :entity <entity>}, {:type :add-agent :agent <agent>}"
  [world event]
  (case (:type event)
    :add-entity (assoc-in world [:entities (:id (:entity event))] (:entity event))
    :add-agent (update world :agent_pool (fn [s] (conj (or s #{}) (:agent event))))
    :damage (let [{:keys [target-id amount]} event
                  ent (get-in world [:entities target-id])]
              (if (nil? ent)
                world
                (let [components (:components ent)
                      hp (or (:hp components) 0)
                      new-hp (- hp amount)
                      new-components (if (>= new-hp 0)
                                       (assoc components :hp new-hp)
                                       (dissoc components :hp))
                      updated-ent (if (>= new-hp 0)
                                    (assoc ent :components new-components)
                                    nil)
                      world1 (if updated-ent
                               (assoc-in world [:entities target-id] updated-ent)
                               (update world :entities dissoc target-id))]
                  world1)))
    world))

(defn commit-event
  "Swap an atom world with the given event using `apply-event` and return the new world." [world-atom event]
  (swap! world-atom apply-event event))
;; Not sure how to do this without atoms, but this makes enough sense for now.

;; a Stage needs to be a structure that somehow contains all of the ambiguous and explicit data about a game





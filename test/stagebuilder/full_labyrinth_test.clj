(ns stagebuilder.full-labyrinth-test
  (:require [clojure.test :refer :all]
            [stagebuilder.generator :as gen]
            [stagebuilder.parser :as parser]))

(deftest generate-medium-labyrinth
  (testing "Generate a 1k x 1k labyrinth"
    (let [lab (gen/generate-labyrinth 1000 1000)]
      (is (= 1000 (:width lab)))
      (is (= 1000 (:height lab)))
      (is (>= (count (:rooms lab)) 10))
      (is (> (count (:corridors lab)) 0))
      (println (str "Generated " (count (:rooms lab)) " rooms"))
      (println (str "Generated " (count (:corridors lab)) " corridor tiles")))))

(deftest save-and-load-medium-labyrinth
  (testing "Generate, save, and load a 1k labyrinth"
    (let [temp-file (java.io.File/createTempFile "medium-labyrinth" ".txt")
          temp-path (.getAbsolutePath temp-file)]
      (try
        ;; Generate and save
        (let [lab (gen/generate-labyrinth 1000 1000)]
          (gen/save-labyrinth lab temp-path)
          (is (= 1000 (:width lab)))
          
          ;; Load it back
          (let [stage (parser/load-stage-from-file temp-path "test-labyrinth")]
            (is (= "test-labyrinth" (:label stage)))
            (is (= 1000 (:width stage)))
            (is (= 1000 (:height stage)))
            (is (> (count (:walkable stage)) 10000))
            (println (str "Generated labyrinth with " 
                         (count (:walkable stage)) " walkable tiles out of " (* 1000 1000) " total tiles ("
                         (format "%.2f" (* 100.0 (/ (count (:walkable stage)) (* 1000 1000)))) "%)"))))
        
        (finally
          (.delete temp-file))))))

(deftest corridor-generation
  (testing "Corridors connect rooms"
    (let [lab (gen/generate-labyrinth 500 400)]
      (is (> (count (:corridors lab)) 0))
      ;; Check that corridors are walkable (space character)
      (let [corridor-tiles (filter (fn [[x y]] 
                                     (= \space (gen/get-tile-at x y lab))) 
                                   (:corridors lab))]
        (is (> (count corridor-tiles) 0))))))

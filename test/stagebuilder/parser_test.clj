(ns stagebuilder.parser-test
  (:require [clojure.test :refer :all]
            [stagebuilder.parser :as parser]
            [stagebuilder.generator :as gen]
            [clojure.java.io :as io]))

(def sample-map-text
  "################
#              #
#              #
#              #
#              #
################")

(deftest parse-ascii-map-basic
  (testing "Parse a simple ASCII map"
    (let [grid (parser/parse-ascii-map sample-map-text)]
      (is (= 16 (:width grid)))
      (is (= 6 (:height grid)))
      (is (map? (:tiles grid)))
      (is (= \# (parser/get-tile grid 0 0)))
      (is (= \space (parser/get-tile grid 1 1))))))

(deftest find-walkable-and-walls
  (testing "Find walkable tiles and walls"
    (let [grid (parser/parse-ascii-map sample-map-text)
          walkable (parser/find-walkable-tiles grid)
          walls (parser/find-walls grid)]
      (is (> (count walkable) 0))
      (is (> (count walls) 0))
      (is (= \space (second (first walkable))))
      (is (= \# (second (first walls)))))))

(deftest grid-to-stage-conversion
  (testing "Convert parsed grid to stage structure"
    (let [grid (parser/parse-ascii-map sample-map-text)
          stage (parser/grid->stage grid "test-stage")]
      (is (= "test-stage" (:label stage)))
      (is (= 16 (:width stage)))
      (is (= 6 (:height stage)))
      (is (set? (:walkable stage)))
      (is (set? (:walls stage)))
      (is (> (count (:walkable stage)) 0)))))

(deftest load-stage-from-generated-file
  (testing "Generate labyrinth, save to file, and reload"
    (let [temp-file (java.io.File/createTempFile "test-labyrinth" ".txt")
          temp-path (.getAbsolutePath temp-file)
          lab (gen/generate-labyrinth 100 80)]
      (try
        ;; Generate and save
        (gen/save-labyrinth lab temp-path)
        
        ;; Load it back
        (let [stage (parser/load-stage-from-file temp-path "loaded-stage")]
          (is (= "loaded-stage" (:label stage)))
          (is (= 100 (:width stage)))
          (is (= 80 (:height stage)))
          (is (> (count (:walkable stage)) 0))
          (is (> (count (:walls stage)) 0)))
        
        (finally
          (.delete temp-file))))))

(ns stagebuilder.map-generation-test
  (:require [clojure.test :refer :all]
            [stagebuilder.generator :as gen]
            [clojure.java.io :as io]))

(def test-maps-dir "test/testmaps")

(defn ensure-test-dir []
  "Create the testmaps directory if it doesn't exist."
  (.mkdirs (io/file test-maps-dir)))

(deftest generate-five-random-maps-test
  (testing "Generate 5 random maps to test/testmaps folder"
    (ensure-test-dir)
    (doseq [i (range 1 6)]
      (let [filename (str test-maps-dir "/random_map_" i ".txt")
            width (+ 50 (rand-int 51))   ; Random size between 50-100
            height (+ 50 (rand-int 51))
            labyrinth (gen/generate-labyrinth width height)]
        (gen/save-labyrinth labyrinth filename)
        (is (.exists (io/file filename))
            (str "Map " i " should be created at " filename))
        (is (pos? (.length (io/file filename)))
            (str "Map " i " should have content"))))))

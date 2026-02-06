(defproject a_game "0.0.1-SNAPSHOT"
  :description "A sweet hack and slash mazelike."
  :url "http://packetfire.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "1.0.0"]]
  :main engine.gui
  :aot [engine.gui engine.game]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :main engine.gui
                       :uberjar-name "a_game-standalone.jar"}
             :console {:main engine.game}})

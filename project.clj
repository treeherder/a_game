(defproject a_game "0.1.0-SNAPSHOT"
  :description "A sweet hack and slash roguelike."
  :url "http://packetfire.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot a-game.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

(defproject gigaword "3.1.1-SNAPSHOT"
  :description "Gigaword Clojure API."
  :url "https://github.com/maxthomas/gigaword"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :pom-addition [:developers [:developer
                              [:id "maxthomas"]
                              [:name "Max Thomas"]
                              [:url "http://www.maxthomas.io"]
                              [:email "max.thomas@jhu.edu"]]]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.9.0"]]

  :main ^:skip-aot gigaword.core

  :global-vars { *warn-on-reflection* true }

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})

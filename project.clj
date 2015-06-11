(defproject gigaword "3.0.1"
  :description "Gigaword Clojure API."
  :url "https://github.com/maxthomas/gigaword"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :pom-addition [:developers [:developer
                              [:id "maxthomas"]
                              [:name "Max Thomas"]
                              [:url "http://www.maxjthomas.com"]
                              [:email "max.thomas@jhu.edu"]]]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.9.0"]]

  :main ^:skip-aot gigaword.core

  :global-vars { *warn-on-reflection* true }

  ;; :source-paths ["src/main/clj"]
  ;; :test-paths ["src/test/clj"]

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})

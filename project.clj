(defproject gigaword "1.0.2"
  :description "Gigaword Clojure/Java API."
  :url "https://github.com/maxthomas/gigaword"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :pom-addition [:developers [:developer
                              [:id "maxthomas"]
                              [:name "Max Thomas"]
                              [:url "http://www.maxjthomas.com"]
                              [:email "max.thomas@jhu.edu"]]]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.9.0"]
                 ;; [com.taoensso/timbre "3.3.1"]
                 ]

  :plugins [[lein-junit "1.1.8"]]

  :main ^:skip-aot gigaword.core

  :global-vars { *warn-on-reflection* true }

  :source-paths ["src/main/clj"]
  :test-paths ["src/test/clj"]

  :java-source-paths ["src/main/java"]
  :junit ["src/test/java"]
  :javac-options ["-target" "1.8" "-source" "1.8"]

  :target-path "target/%s"

  :profiles {:dev {:dependencies [[junit/junit "4.11"]]
                   :resource-paths ["src/main/resources"]
                   :java-source-paths ["src/main/java" "src/test/java"]}
             :uberjar {:aot :all}})

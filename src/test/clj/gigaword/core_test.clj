(ns gigaword.core-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [gigaword.core :as giga]
            [clojure.java.io :refer [resource file]]))


(deftest docline-parsing
  (is (= {:id "NYT_ENG_20010901.0015"
          :type "story"}
         (giga/docline->map docline))))

(deftest nospace-parsing
  (is (= {:id "WPB_ENG_20100901.0001"
          :type "story"}
         (giga/docline->map nospace))))

(deftest docline-parsing
  (is (= {:id "NYT_ENG_20010901.0015"
          :type "story"}
         (giga/docline->map docline))))

(defn slurp-resource [^String path]
  (->> path
       resource
       file
       slurp))

(defn resource->clj [^String path]
  (->> path
       slurp-resource
       giga/proxy->map))

(def dbm-path "serif_dog-bites-man.sgml")

(deftest dog-bites-man
  (let [test (resource->clj dbm-path)]
    (is (= [[0 14] [14 85] [85 94] [94 133]]
           (:sections test)))))

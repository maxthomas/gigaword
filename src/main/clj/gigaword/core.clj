;; Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
;; See LICENSE in the project root directory.
(ns gigaword.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clj-time.local :refer [to-local-date-time]]
            [clj-time.format :refer [formatter parse]]
            [clj-time.coerce :refer [from-long to-long]]
            ;; [taoensso.timbre :as timbre]
            ))

;; (timbre/refer-timbre)

;; The format of dates in LDC SGML IDs.
(def giga-dt-formatter (formatter "yyyyMMdd"))

(defn extract-date-str
  "Extract the date string from a LDC SGML ID string."
  [^String id-str]
  (let [spl (->> (clojure.string/split id-str #"_")
                 (last))
        dt-str (->> (str/split spl #"\.")
                    (first))]
    dt-str))

(defn id->millis
  "Given a Gigaword/LDC SGML id string, extract a date in milliseconds in local time."
  [^String id]
  (->> id
       (extract-date-str)
       (parse giga-dt-formatter)
       (to-local-date-time)
       (to-long)))

(defn not-nil-but-empty?
  "Check to see if a string is both not nil and empty."
  [^String s]
  (and (not (nil? s))
       (empty? (->> s
                    str/trim))))

(defn repair-nbs
  "Repair any non-breaking whitespace. Occasionally present in Gigaword documents."
  [^String proxy-str]
  (str/replace proxy-str #"\p{Zs}" " "))

(defn classify-passage
  "Determine what type of section content that this line is."
  [ptag ^String ln]
  (cond
    ;; If the line starts with </, this is an endtag.
    (.startsWith ln "</")    :endtag

    ;; If the line starts with <HEADLINE>, this is a headline tag.
    (= "<HEADLINE>" ln)      :hltag

    ;; If the previous tag was HEADLINE, this is a headline section.
    (= :hltag ptag)          :headline

    ;; If the line starts with <DATELINE>, this is a dateline tag.
    (= "<DATELINE>" ln)      :dltag

    ;; If the previous tag was a DATELINE, this is a dateline section.
    (= :dltag ptag)          :dateline

    ;; If the line starts with <, it is a start tag.
    (.startsWith ln "<")     :starttag

    ;; Otherwise, it's a passage.
    :else                    :passage))

(defn infer-sections
  "Establish text spans for an SGML document. Does not mutate the text."
  ([lines cctr]
   (infer-sections lines [] :docline cctr))
  ([lines sections tag cctr]
   (if (= 0 (count lines))
     ;; Return if no more lines.
     sections
     (let [ln (first lines)
           ct (count ln)
           elen (+ cctr ct)
           nctr (+ cctr ct 1)
           rest (pop lines)]

       (infer-sections
        rest
        (conj sections {:b cctr
                        :e elen
                        :t tag})
        (classify-passage tag ln)
        nctr)))))

(defn process-docline
  "Process the <DOC...> line (first line) of LDC SGML documents."
  [^String docline]
  (let [spl (str/split docline #"\"")
        id (nth spl 1)]
    {:id id
     :date (id->millis id)
     :kind (nth spl 3)}))

(defn process-ldc-sgml
  "Given a string of LDC SGML, return a map with keys:
  :id - the ID
  :date - milliseconds since the epoch in local time
  :kind - the type of document
  :sections - an array of maps, representing a span of text:
    :b - the beginning of the span
    :e - the end
    :t - the type (e.g., :passage)"
  [^String sgml]
  (let [lines (->> sgml
                   (str/split-lines))
        fl (->> lines
                first)
        dllen (+ (count fl)
                 1)
        dlmap (->> fl
                   process-docline)
        sects (->> lines
                   rest
                   reverse
                   (into ())
                   (#(infer-sections % dllen))
                   (filter (fn [_]
                             (let [{tag :t} _]
                               (or (= tag :headline)
                                   (= tag :dateline)
                                   (= tag :passage)))))
                   (into []))]
    (conj dlmap
          {:sections sects})))

(defn -main
  "Ingest and print an LDC SGML file."
  [& args]
  (let [res (->> args
                 first
                 slurp
                 process-ldc-sgml)]
    (with-out-str
      (clojure.pprint/pprint res))))

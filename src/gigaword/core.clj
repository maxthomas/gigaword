;; Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
;; See LICENSE in the project root directory.
(ns gigaword.core
  (:gen-class)
  (:import [java.util.zip GZIPInputStream])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clj-time.local :refer [to-local-date-time]]
            [clj-time.format :refer [formatter parse]]
            [clj-time.coerce :refer [from-long to-long]]))

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
   (infer-sections
    lines
    :docline
    cctr
    [{:b 0 :e (- cctr 1) :t :docline}]))
  ([lines tag cctr sections]
   (if (= 0 (count lines))
     ;; Return if no more lines.
     sections

     (let [ln (first lines)
           rest (pop lines)
           ct (count ln)
           elen (+ cctr ct)
           newtag (classify-passage tag ln)

           pnext (partial infer-sections rest
                          newtag
                          (+ elen 1))

           lastsect (->> sections
                         peek)
           pst (->> lastsect
                    :t)]

       ;; If the previous section was a passage
       ;; and this section is a passage, merge them.
       (if (and (= :passage pst)
                (= :passage newtag))
         (let [nolastsect (->> sections
                               pop)
               nsects (->> (assoc lastsect :e elen)
                           (conj nolastsect))]
           (recur rest
                  newtag
                  (+ elen 1)
                  nsects))
         (recur rest
                newtag
                (+ elen 1)
                (conj sections {:b cctr
                                :e elen
                                :t newtag})))))))

;; (infer-sections
;;  newtag
;;  (+ elen 1)
;;  (conj sections {:b cctr
;;                  :e elen
;;                  :t newtag}))

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
                             (let [{t :t} _]
                               (or (= :headline t)
                                   (= :dateline t)
                                   (= :passage t)))))
                   (into []))]
    (conj dlmap
          {:sections sects})))

(defn lines->docs
  "Take concatenated SGML documents and return a vector
  of strings representing individual SGML documents."
  ([lines]
   (lines->docs [] [] lines))
  ([cdoc docs lines]
   ;; if lines are empty, return docs.
   (if (empty? lines)
     docs
     (let [^String f (first lines)
           r (rest lines)]
       ;; If the line begins with the document start,
       ;; start a new document by recursing.
       (if (.startsWith f "<DOC id=")
         (recur [f]
                docs
                r)

         ;; otherwise, add the lines to the current doc.
         (let [ndoc (conj cdoc f)]
           ;; If the tag is the end document tag, this document is finished.
           (if (= "</DOC>" f)
             (do
               (recur []
                      (conj docs
                            (str/join "\n" ndoc))
                      r))

             ;; otherwise, keep recursing.
             (recur ndoc
                    docs
                    r))))))))

(defn gz->docs
  "Take a string path and convert it to
  a sequence of strings representing individual SGML documents."
  [^String path]
  (with-open [in (io/input-stream path)
              gz (new GZIPInputStream in)
              rdr (io/reader gz)]
    (->> rdr
         line-seq
         lines->docs)))

(defn -main
  "Ingest and print an LDC SGML file."
  [& args]
  (let [res (->> args
                 first
                 slurp
                 process-ldc-sgml)]
    (pp/pprint res)))

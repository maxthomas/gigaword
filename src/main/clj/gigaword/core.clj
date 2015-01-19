;;
;; Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
;; See LICENSE in the project root directory.
;;
(ns gigaword.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clj-time.local :refer [to-local-date-time]]
            [clj-time.core :refer [date-time now millis]]
            [clj-time.format :refer [formatter parse]]
            [clj-time.coerce :refer [from-long to-long]]))

(defn path->gzip->str [^String path]
  (with-open [r (->> path
                     (java.io.FileInputStream.)
                     (java.util.zip.GZIPInputStream.))]
    (slurp r)))

(def giga-dt-formatter (formatter "yyyyMMdd"))

(defn extract-date-str [^String id-str]
  (let [spl (->> (clojure.string/split id-str #"_")
                 (last))
        dt-str (->> (str/split spl #"\.")
                    (first))]
    dt-str))

(defn id->millis [^String id]
  (->> id
       (extract-date-str)
       (parse giga-dt-formatter)
       (to-local-date-time)
       (to-long)))

(defn add-date [pm]
  (->> pm
       :id
       id->millis
       (hash-map :millis)
       (merge pm)))

(defn repair-text [{:keys [sections] :as om}]
  (assoc om :text (str/join sections)))

(defn append-headline [{:keys [sections headline] :as om}]
  (if (not (nil? headline))
    (let [firststr (str headline "\n")
          newsects (cons firststr sections)]
      (assoc om :sections newsects))
    om))

(defn append-dateline [{:keys [sections dateline] :as om}]
  (if (not (nil? dateline))
    (let [firststr (str dateline "\n")
          newsects (cons firststr sections)]
      (assoc om :sections newsects))
    om))

(defn process-paras
  ([lines]
     (process-paras lines [] []))
  ([lines current-para paras]
     (if-let [next (first lines)]
       (cond
        (= next "<P>")
        (process-paras (rest lines)
                       []
                       paras)
        (= next "</P>")
        (process-paras (rest lines)
                       []
                       (conj paras current-para))
        ;; else --> text
        :else
        (process-paras (rest lines)
                       (conj current-para (str next "\n"))
                       paras))
       paras)))

(defn text->sections [{:keys [^String text] :as om}]
  (let [lines (->> text
                   clojure.string/split-lines)]
    (if (.contains text "<P>")
      (let [sections (->> lines
                          process-paras
                          (map str/join)
                          (filter #(not (empty? (str/trim %))))
                          (into []))]
        (assoc om :sections sections))
      (assoc om :sections [text]))))

(defn kill-quotes [^String s]
  (.replaceAll s "\"" ""))

(defn docline->map [^String docline]
  (->> (clojure.string/split docline #" ")
       (filter (fn [^String _]
                 (.contains _ "=")))
       (map #(clojure.string/split % #"="))
       (map (fn [[f s]]
              (hash-map (keyword f)
                        (kill-quotes s))))
       (reduce merge)))

(defn lines->text [lines]
  (->> lines
       (filter #(not (or (= "<TEXT>" %)
                         (= "</TEXT>" %)
                         (= "</P>" %)
                         (= "</DOC>" %))))
       (drop 1)
       (map (fn [^String _]
              (if (= "<P>" _)
                ""
                _)))
       ;; (filter #(not= "" %))
       (str/join "\n")))

(defn surround-tag [^String tag]
  (str "<" tag ">"))

(defn end-tag [^String open-tag]
  (->> (subs open-tag 1)
       (str "</")))

(defn get-between-tags
  ([lines]
     (let [endtag (->> lines
                       first
                       (end-tag))]
       (get-between-tags (drop 1 lines)
                         endtag
                         []
                         1)))
  ([lines ^String endtag fill ctr]
     (if-let [current (first lines)]
       (cond
        ;; if end tag, return.
        (= endtag current)
        (let [headline (->> fill
                            (str/join "\n"))]
          [(inc ctr) headline])

        ;; Otherwise, add to fill and recurse.
        :else
        (get-between-tags (drop 1 lines)
                          endtag
                          (conj fill current)
                          (inc ctr)))
       (println (str "Error - did not find end tag: " endtag)))))

(defn process-doc
  ([lines]
     (process-doc lines {:headline nil
                         :dateline nil}))
  ([lines data]
     (if-let [head (first lines)]
       (cond
        (= (subs head 0 4) "<DOC")
        (process-doc (rest lines)
                     (->> head
                          docline->map
                          (merge data)))
        (= head "<HEADLINE>")
        (let [[ctr processed] (get-between-tags lines)]
          (process-doc (drop ctr lines)
                       (assoc data :headline processed)))

        (= head "<DATELINE>")
        (let [[ctr processed] (get-between-tags lines)]
          (process-doc (drop ctr lines)
                       (assoc data :dateline processed)))

        (= head "<TEXT>")
        (let [[ctr processed] (get-between-tags lines)]
          (process-doc (drop ctr lines)
                       (assoc data :text processed)))

        :else
        data)
       data)))

(defn create-content [{:keys [content-list] :as om}]
  (assoc om :content (->> content-list
                          (str/join "\n"))))

(defn span-list [{:keys [sections] :as om}]
  (let [counts (->> sections
                    (map count))
        zeroed (conj counts 0)]
    (reductions + zeroed)))

(defn span-list->spans
  ([list]
     (span-list->spans (first list)
                       (rest list)
                       []))
  ([curr list stack]
     (if-let [f (first list)]
       (span-list->spans f
                         (rest list)
                         (conj stack [curr f]))
       stack)))

(defn sections->spans [om]
  (assoc om :sections (->> om
                           span-list
                           span-list->spans
                           (filter (fn [[x y]]
                                     (not= x y))))))

(defn not-nil-but-empty?
  "Check to see if a string is both nil and non-empty."
  [^String s]
  (and (not (nil? s))
       (empty? (->> s
                    str/trim))))

(defn fix-empty-headline [{:keys [headline] :as orig}]
  (if (not-nil-but-empty? headline)
    (assoc orig :headline nil)
    orig))

(defn fix-empty-dateline [{:keys [dateline] :as orig}]
  (if (not-nil-but-empty? dateline)
    (assoc orig :dateline nil)
    orig))

(defn repair-nbs [^String proxy-str]
  (str/replace proxy-str #"\p{Zs}" " "))

(defn proxy->map [^String proxy-str]
  (->> proxy-str
       repair-nbs
       str/split-lines
       process-doc
       fix-empty-headline
       fix-empty-dateline
       add-date
       text->sections
       append-dateline
       append-headline
       repair-text
       sections->spans
       (#(assoc % :raw (str/trim proxy-str)))))

(defn path->map [^String proxypath]
  (->> proxypath
       slurp
       proxy->map))

(defn gigazip->proxystrs [^String gzip-path]
  (->> gzip-path
       (path->gzip->str)
       (#(str/split % #"</DOC>\n"))))

(defn gigazip->clj [^String gzip-path]
  (->> gzip-path
       (gigazip->proxystrs)
       (map #(str % "</DOC>"))
       (map str/trim)
       (map proxy->map)))

(defn paths->cljs [^String path-to-paths]
  (->> path-to-paths
       (slurp)
       (clojure.string/split-lines)
       (pmap gigazip->clj)
       (flatten)))

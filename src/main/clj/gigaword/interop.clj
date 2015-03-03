;;
;; Copyright 2012-2015 Johns Hopkins University HLTCOE. All rights reserved.
;; See LICENSE in the project root directory.
;;
(ns gigaword.interop
  (:require [gigaword.core :as giga])
  (:import [gigaword.interfaces GigawordDocument TextSpan]
           [gigaword GigawordDocumentType]
           [java.util ArrayList Optional]))

(defn tuple->cts [[o t]]
  (reify TextSpan
    (getStart [this]
      o)
    (getEnding [this]
      t)))

(defn create-spans
  ([stack]
     (create-spans 0 stack []))
  ([prev stack output]
     (if-let [current (first stack)]
       (do
         (create-spans current
                       (rest stack)
                       (conj output [prev current])))
       output)))

(defn clj->pc [{:keys [^String text
                       ^String type
                       ^long millis
                       ^String id
                       headline dateline sections]}]
  (reify GigawordDocument
    (getText [this]
      text)
    (getType [this]
      (->> type
           .toUpperCase
           GigawordDocumentType/valueOf))
    (getMillis [this]
      millis)
    (getId [this]
      id)
    (getDateline [this]
      (Optional/ofNullable dateline))
    (getHeadline [this]
      (Optional/ofNullable headline))
    (getTextSpans [this]
      (->> sections
           (map tuple->cts)))))

(defn proxydoc->pc [^String path]
  (->> path
       giga/path->map
       clj->pc))

(defn proxystr->pc [^String proxystr]
  (->> proxystr
       giga/proxy->map
       clj->pc))

(defn gigazip->pcs [^String path]
  (->> path
       giga/gigazip->clj
       (map clj->pc)))

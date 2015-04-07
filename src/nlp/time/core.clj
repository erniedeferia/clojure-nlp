(ns nlp.time.core
  "Core date-time related vars and functions."
  (:use [opennlp.nlp])
  (:require
   [clojure.string :refer [join] :as cljstr]
   [clj-time.core :as t :only [plus months weeks]]
   [clj-time.format :as ttf :only [formatter unparse]]
   [clj-time.local :as l :only [local-now]]))

(def date-find     (delay (make-name-finder "models/en-event-datetime.bin")))

;; Date nouns. The associated indexes match those defined for
;; day-of-week in the clj-time library, except of 'today'
;; and 'tomorrow', which are my own.
(def date-nouns {
                 "today"    -1
                 "tomorrow"  0
                 "monday"    1
                 "tuesday"   2
                 "wednesday" 3
                 "thursday"  4
                 "friday"    5
                 "saturday"  6
                 "sunday"    7
                 })

(def multi-parser (ttf/formatter
                   (t/default-time-zone)
                   "yyyy-MM-dd @ hh:mma"
                   "YYYY/MM/dd @ hh:mma"
                   "MMM d yyyy @ hh:mma"
                   "MMM d yyyy @ hh:mm a"
                   "YYYY/MM/dd 'at' h:mma"
                   "MMM d yyyy"
                   "MMM d yyyy 'at' h:mma"
                   "MMM d yyyy 'at' h:mm a"
                   "MMM d yyyy h:mma"
                   "MMM d yyyy h:mm a"))

(def standard-date-formatter (ttf/formatter
                              "MMMM d yyyy"
                              (t/default-time-zone)))

(def standard-datetime-formatter (ttf/formatter
                                  "MMMM d yyyy 'at' h:mm a"
                                  (t/default-time-zone)))


(defn date-from-noun
  "Converts one of the date nounts (e.g., tomorrow) into the proper
   date object, using local time."
  [noun]
  ;; get the index associated with the noun. If not found, return
  ;; the one for today (-1).
  (let [noun-idx (get date-nouns (cljstr/lower-case noun) -1)
        today (t/today-at-midnight)
        today-idx (t/day-of-week today)
        ]
    (cond
     (= noun-idx -1)
       today
     (= noun-idx 0)
       (t/plus- today (t/days 1))
     (> noun-idx 0)
       (t/plus- today (t/days (- noun-idx today-idx)))
     )))

(defn clean-datetime
  [sdate]
  (->
    ;; git rid of . from a.m. or p.m.
   (cljstr/replace sdate "." ""))
  )

(defn find-datetime-tokens
  "Finds date/time related tokens from a tokenized sentence and returns a lazy sequence of them."
  [tokens]
  (@date-find tokens))


(defn stringify-datetime-tokens
  "Joins all date & time related tokens into a single string."
  [tokens]
  (clojure.string/join " " tokens))

(defn parse-datetime
  "Parse a date string into a valid datetime object."
  [s-date]
  (ttf/parse multi-parser s-date)
)

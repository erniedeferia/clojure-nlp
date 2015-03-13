(ns nlp.core
  (:use [clojure.pprint :as pp]
        [opennlp.nlp]
       ;; [opennlp.treebank]
        )
  (:require
   [clojure.string :only [split] :as str]
   [clj-time.format :as ttf]
   [clj-time.local :as l]
   [clj-time.core :as tt :exclude [second]])
  (:gen-class))

(def get-sentences (make-sentence-detector "models/en-sent.bin"))
(def tokenize      (make-tokenizer "models/en-token.bin"))
(def detokenize    (make-detokenizer "models/english-detokenizer.xml"))
(def pos-tag       (make-pos-tagger "models/en-pos-maxent.bin"))
(def name-find     (make-name-finder "models/namefind/en-ner-person.bin"))
(def date-find     (make-name-finder "models/namefind/en-ner-date.bin"))
(def time-find     (make-name-finder "models/namefind/en-ner-time.bin"))
;;(def duration-find (make-name-finder "models/en-duration.bin"))

(def multi-parser (ttf/formatter (tt/default-time-zone) "yyyy-MM-dd @ hh:mma" "YYYY/MM/dd @ hh:mma" "MMM d yyyy @ hh:mma"  "YYYY/MM/dd 'at' hh:mma" "MMM d yyyy 'at' hh:mma" ))


(defn find-persons
  "Finds any properly capitalized names from a tokenized sentence."
  [tokens]
  (name-find tokens))

(defn find-datetime-tokens
  "Finds date/time related tokens from a tokenized sentence and returns a lazy sequence of them."
  [tokens]
  (date-find tokens))

(defn find-datetime-tokens-wrapper
  [tokens]
  (let [ date-tokens (find-datetime-tokens tokens)]
    (pp/pprint date-tokens)
    date-tokens
       )
  )

(defn unify-datetime-tokens
  "Joins all date & time related tokens into a single string."
  [tokens]
  (clojure.string/join " " tokens))

(defn parse-datetime
  "Parse a date string into a valid datetime object."
  [s-date]
  (ttf/parse multi-parser s-date)
  )

(defn parse-duration
  "Looks for tokens that specify the duration of the meeting. We use a very simple approach:
   1. find location of the 'for' token.
   2. assume the next two tokens are of the form 'N [minutes|hours]"
  [tokens]
  (let [duration_index (.indexOf tokens "for")
        duration (nth tokens (+ duration_index 1))
        dimension (nth tokens (+ duration_index 2))
        ]
    {:duration duration :time dimension}
    )
  )

(comment
(defn parse-duration
  [tokens]
  (let [duration-tokens (str/split (first (duration-find tokens))  #"\s+") ]
    (pp/pprint duration-tokens)
    (pp/pprint (second duration-tokens))
    (pp/pprint (nth duration-tokens 1))
    {:duration (second duration-tokens) :time (nth duration-tokens 2) }
       )
  )

  )

(defn parse-message
  "Parse the message and extract people and start time for the calendar event.
   Return a map with {:people :starts-at}."
  [s]
  (let [tokens      (tokenize s)
        people      (find-persons tokens)
        starts-at   (-> tokens find-datetime-tokens-wrapper unify-datetime-tokens parse-datetime)
        start-time  (parse-datetime (unify-datetime-tokens (find-datetime-tokens tokens)))
        duration    (parse-duration tokens)
        ]
       {:people people :starts-at starts-at :duration duration}
    )
  )

(defn -main
  "I don't do a whole lot ... yet. But here are a couple of sample OpenNPL functions in action:"
  [& args]

  )

(load "training")

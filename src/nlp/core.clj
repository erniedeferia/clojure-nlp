(ns nlp.core
  (:use [clojure.pprint :as pp]
        [opennlp.nlp]
        )
  (:require
   [nlp.time.core :as xtime]
   [clojure.string :only [split replace] :as str]
   [clj-time.format :as ttf]
   [clj-time.local :as l]
   [clj-time.core :as tt :exclude [second]])
  (:gen-class))

(def get-sentences (delay (make-sentence-detector "models/en-sent.bin")))
(def tokenize      (delay (make-tokenizer "models/en-token.bin")))
(def detokenize    (delay (make-detokenizer "models/english-detokenizer.xml")))
(def pos-tag       (delay (make-pos-tagger "models/en-pos-maxent.bin")))
(def name-find     (delay (make-name-finder "models/namefind/en-ner-person.bin")))
(def duration-find (delay (make-name-finder "models/en-duration.bin")))

(defn find-persons
  "Finds any properly capitalized names from a tokenized sentence."
  [tokens]
  (@name-find tokens))


(defn parse-duration
  [tokens]
  (let [duration-tokens (str/split (first (@duration-find tokens) )  #"\s+") ]
    {:length (first duration-tokens) :time-unit (second duration-tokens) }
       ))

(defn parse-message
  "Parse the message and extract people and start time for the calendar event.
   Return a map with {:people :starts-at}."
  [s]
  (let [tokens      (@tokenize s)
        people      (find-persons tokens)
        starts-at   (-> tokens xtime/find-datetime-tokens
                        xtime/stringify-datetime-tokens
                        xtime/clean-datetime
                        xtime/parse-datetime )
        duration    (-> tokens parse-duration )
        ]
       {:people people :starts-at starts-at :duration duration}
    ))

(defn -main
  "I don't do a whole lot ... yet. But here are a couple of sample OpenNPL functions in action:"
  [& args]

  )

;;(load "training")
;;(load "timetraining")

(ns nlp.training-test
  "Tests the calendar event Duration model creation tool."
  (:require [clojure.test :refer :all]
            [opennlp.nlp :as nlp]
            [clj-time.core :as tt :exclude [second]]
            [nlp.training.duration :as train :refer :all]))


(defn create-model-wrapper
  [f]
  (train/create-duration-model)
  (f)
  )

;; This fixture should be enabled (uncomment it) if the model hasn't been
;; generated. Otherwise, keep it disable - it is expensive.
;;(use-fixtures :once create-model-wrapper)


(deftest has-duration-and-extracts-them
  (let [sentences ["Schedule a meeting with Marie Curie on November 22 2015 at 8:30pm for 2 hours to discuss X."
                   "Schedule a meeting with Adam Smith on November 22 2015 at 7:30pm for 3 hours to discuss Y."
                   "Need a meeting with Monica Bellucci on Sep 21 2015 at 2:30PM for 30 minutes to discuss Z."
                   "Schedule a meeting with Adam Smith on November 22 2015 at 3:30PM for 2 hours to discuss Y."
                   "Schedule a meeting with Adam Smith on November 22 2015 at 12:00AM for 3 hours to discuss Y."
                   "Schedule a meeting with Adam Smith on November 22 2015 at 2:00AM for 3 hours to discuss Y."
                   "Schedule a meeting with Adam Smith on November 22 2015 at 8:30PM for 5 hours  to discuss Y."
                  ]
        name-find (nlp/make-name-finder "models/en-duration.bin")
        tokenizer (nlp/make-tokenizer "models/en-token.bin")
        ]
   (doseq [s sentences]
    (is (= 1 (count (name-find (tokenizer s)))))
    )))

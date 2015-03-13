(ns nlp.training-test
  "Tests the calendar event Duration model creation tool."
  (:require [clojure.test :refer :all]
            [opennlp.nlp :as nlp]
            [nlp.training :as train :refer :all]))


(defn create-model-wrapper
  [f]
  (train/create-duration-model)
  (f)
  )

;; This fixture should be enabled (uncomment it) if the model hasn't been
;; generated. Otherwise, keep it disable - it is expensive.
;;(use-fixtures :once create-model-wrapper)


(deftest has-duration-but-fails-to-extract-due-to-leading-comma
  "These sentences fail because of the comman in front of the duration fragment. "
    (let [sentences ["Schedule a meeting with Adam Smith, on November 22 105 at 02:30PM, for 3 hours to dicuss Y."
                     "Schedule a meeting with Adam Smith on November 22 105 at 01:30PM, for 3 hours to dicuss Y."
                  ]
        name-find (nlp/make-name-finder "models/en-duration.bin")
        tokenizer (nlp/make-tokenizer "models/en-token.bin")
        ]
  (doseq [s sentences]
    (is (= 0 (count (name-find (tokenizer s)))))
    ) )
  )

(deftest has-duration-but-fails-to-extract-due-to-overfitting
    (let [sentences ["This meeting is for 3 hours."
                     "Another meeting will be 5 hours."
                     "And this one will be, for 15 minutes, only."
                  ]
        name-find (nlp/make-name-finder "models/en-duration.bin")
        tokenizer (nlp/make-tokenizer "models/en-token.bin")
        ]
  (doseq [s sentences]
    (is (= 0 (count (name-find (tokenizer s)))))
    )))



(deftest has-duration-and-extracts-them
  (let [sentences ["Schedule a meeting with Marie Curie on November 22 2015 at 08:30pm for 2 hours to discuss X."
                   "Schedule a meeting with Adam Smith, on November 22 105 at 07:30pm for 3 hours to discuss Y."
                   "Need a meeting with Monica Bellucci on Sep 21 2015 at 02:30PM for 30 minutes to discuss Z."
                   "Schedule a meeting with Adam Smith, on November 22 2015, at 01:30PM for 2 hours to discuss Y."
                   "Schedule a meeting with Adam Smith, on November 22 2015, at 12:00PM for 3 hours to discuss Y."
                   "Schedule a meeting with Adam Smith, on November 22 2015, at 10:00AM for 3 hours, to discuss Y."
                   "Schedule a meeting with Adam Smith, on November 22 2015, at 08:30PM for 5 hours , to discuss Y."
                  ]
        name-find (nlp/make-name-finder "models/en-duration.bin")
        tokenizer (nlp/make-tokenizer "models/en-token.bin")
        ]
  (doseq [s sentences]
    (is (= 1 (count (name-find (tokenizer s)))))
    ) ))

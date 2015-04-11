(ns nlp.training.time
  "Function to train a better (than the NER provided by OpenNLP)
   date/time model."
  (:use [nlp.training.core :as traincore ]
        [nlp.time.core :as timec]
        [nlp.math-utils :as utils])
  (:require
   [opennlp.nlp :as nlp]
   [opennlp.tools.train :as train]
   [clojure.string :refer [join] :as cljstr]
   [clojure.java.io :as io]
   [clj-time.core :as t :only [plus months weeks]]
   [clj-time.format :as ttf :only [formatter unparse]]
   [clj-time.local :as l :only [local-now]]
   )
  (:import [java.io File FileOutputStream])
 )

(def datetime-formats ["MMM d yyyy 'at' h:mma"
                       "MMM d yyyy 'at' h:mm a"
                       "MMMM d yyyy 'at' h:mma"
                       "MMMM d yyyy 'at' h:mm a"])


(def formatters (map #(ttf/formatter %) datetime-formats) )

(defn- rand-datetime
  [formats]
  (clj-time.format/unparse
    (rand-nth formats)
    (clj-time.format/parse traincore/custom-formatter (traincore/generate-datetime)))
  )

(defn generate-datetime-tagged
  "Randomly generates a datetime or a date noun (today, tomorrow, Monday, etc.)
   and wraps it with an named entity recognition tag."
  []
  (utils/select-randomly-between
   (str "<START:datetime> " (rand-datetime formatters) " <END>")
   (str "<START:datetime> " (utils/select-randomly-among (keys timec/date-nouns ) ) " <END>")
   ))

(defn generate-datetime-for-testing
  "Randomly generates a datetime or a date noun (today, tomorrow, Monday, etc.)
   without the NER tag - for testing purposes."
  []
  (utils/select-randomly-between
   (str (rand-datetime formatters) )
   (str (utils/select-randomly-among (keys timec/date-nouns ) ) )
   ))


(defn generate-sentences-for-datetime-training
  "Generate and writes [count] training sentences to [file],
   one per line."
  [cnt filename]
  ;; coerse the filename parameter to a string to avoid being
  ;; mistaken for input stream.
  (let [file_name (str filename)
        last-names (traincore/read-file traincore/last-name-file)
        first-names (traincore/read-file traincore/first-name-file)
        request-clauses (traincore/read-file traincore/request-clause-file)
        subject-clauses (traincore/read-file traincore/subject-clause-file)
        data {:data-lastnames last-names
              :data-firstname last-names
              :data-requests request-clauses
              :data-subjects subject-clauses}
        generators {:gen-datetime generate-datetime-tagged
                    :gen-duration traincore/generate-duration
                    :gen-subject  traincore/generate-subject
                    :gen-request  traincore/generate-request-clause}
        ]
    (with-open [wrt (io/writer file_name )]
      (doseq [sentence (take cnt
                             (repeatedly
                              #(traincore/generate-sentence data generators))) ]
        (.write wrt (str sentence "\n" )) ;; write line to file
        ))))


(defn train-datetime-model
  [training-filename output-filename]
  (let [datetime-finder-model (train/train-name-finder training-filename)]
    (traincore/store-model-file datetime-finder-model
                                   output-filename)
    ))

(defn create-event-datetime-model
  []
  (let [sentences-filename "models/en-event-datetime.sentences"
        sentences-count 20000
        output-filename "models/en-event-datetime.bin"
        ]
    (generate-sentences-for-datetime-training sentences-count
                                              sentences-filename)
    (train-datetime-model sentences-filename
                          output-filename)))


(defn test-event-datetime-model
  "Cross-validates the model by generating a set of sentences using the
   same rules as those used for training and then using the trained
   model to extract the DateTime entity from each. The efficacy of the
   model is described by the success/total ratio."
  [sample-count]
  (let [
        datetime-find     (nlp/make-name-finder "models/en-event-datetime.bin")
        last-names        (traincore/read-file  traincore/last-name-file)
        first-names       (traincore/read-file  traincore/first-name-file)
        request-clauses   (traincore/read-file  traincore/request-clause-file)
        subject-clauses   (traincore/read-file  traincore/subject-clause-file)
        data {:data-lastnames last-names
              :data-firstname last-names
              :data-requests  request-clauses
              :data-subjects  subject-clauses}
        generators {:gen-datetime generate-datetime-for-testing
                    :gen-duration traincore/generate-duration
                    :gen-subject  traincore/generate-subject
                    :gen-request  traincore/generate-request-clause}
        success   (reduce +
                           (take sample-count
                                 (repeatedly
                                  #(count (datetime-find
                                           (@nlp.core/tokenize
                                            (traincore/generate-sentence
                                                 data
                                                generators)))))))]
    (/ (float success) (float sample-count))
    )
  )

(defn train []
  (do
    (create-event-datetime-model)
    (println "Validating the model...")
    (test-event-datetime-model 100)
    ))

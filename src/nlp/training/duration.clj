(ns nlp.training.duration
  (:use [nlp.training.core :as traincore] )
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

(defn generate-duration-tagged
  "Generates the possible or likely phrases for the duration
   of a calendar event with the appropriate tagging for
   training."
  []
  (str " <START:duration> " (traincore/generate-duration) " <END> ")
  )


(defn generate-sentences-for-duration-training
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
        generators {:gen-datetime traincore/generate-datetime
                    :gen-duration generate-duration-tagged
                    :gen-subject  traincore/generate-subject
                    :gen-request  traincore/generate-request-clause}
        ]
    (println "> generating sentences for duration training...")
    (with-open [wrt (io/writer file_name )]
      (doseq [sentence (take cnt
                             (repeatedly
                              #(traincore/generate-sentence data generators))) ]
        (.write wrt (str sentence "\n" )) ;; write line to file
        ))
    )
  )

(defn- train-duration-model
  "Trains a name-finder model using sentences that have been benerated by the
   (generate-sentences) form."
  [training-filename output-filename]
  (let [duration-finder-model (train/train-name-finder training-filename)]
      (traincore/store-model-file duration-finder-model output-filename)
    )
  )

(defn create-duration-model
  "Create the calendar event duration model. It first creates 15K sample sentences.
  Then trains a name-finder model with them. The output is stored in the models/
  folder."
  []
  (let [sentences-filename "models/duration-training-sentences"
        sentences-count 15000
        output-filename "models/en-duration.bin"]
    (generate-sentences-for-duration-training sentences-count sentences-filename)
    (train-duration-model sentences-filename output-filename)
    ))

(defn test-duration-model
  "Cross-validates the model by generating a set of sentences using the
   same rules as those used for training and then using the trained
   model to extract the Duration entity from each. The efficacy of the
   model is described by the success/total ratio."
  [sample-count]
  (let [
        last-names (traincore/read-file traincore/last-name-file)
        first-names (traincore/read-file traincore/first-name-file)
        request-clauses (traincore/read-file traincore/request-clause-file)
        subject-clauses (traincore/read-file traincore/subject-clause-file)
        data {:data-lastnames last-names
              :data-firstname last-names
              :data-requests request-clauses
              :data-subjects subject-clauses}
        generators {:gen-datetime traincore/generate-datetime
                    :gen-duration traincore/generate-duration
                    :gen-subject  traincore/generate-subject
                    :gen-request  traincore/generate-request-clause}
        success   (reduce +
                           (take sample-count
                                 (repeatedly
                                  #(count (@nlp.core/duration-find
                                           (@nlp.core/tokenize
                                            (traincore/generate-sentence
                                                 data
                                                generators)))))))]
    (/ (float success) (float sample-count))
    )
  )

(defn train []
  (do
    (create-duration-model)
    (println "Validating model...")
    (test-duration-model 100)
    )
  )

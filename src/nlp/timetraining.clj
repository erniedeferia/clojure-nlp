(ns nlp.timetraining
  "Function to train a better (then the NER provided by OpenNLP)
   date/time model."
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
                       "MMMM d yyyy 'at' hh:mma"
                       "MMM d yyyy 'at' h:mm a"
                       "MMMM d yyyy 'at' h:mm a"])

(def custom-formatter
     (ttf/formatter "MMMM d yyyy 'at' hh:mma"))

(def formatters (map #(ttf/formatter %) datetime-formats) )

(defn- rand-datetime
  [formats]
  (clj-time.format/unparse
    (rand-nth formats)
    (clj-time.format/parse custom-formatter (nlp.training/generate-datetime)))
  )

(defn generate-datetime-tagged
  []
  (str "<START:datetime> " (rand-datetime formatters) " <END>")
  )


(defn generate-sentences-for-datetime-training
  "Generate and writes [count] training sentences to [file],
   one per line."
  [cnt filename]
  ;; coerse the filename parameter to a string to avoid being
  ;; mistaken for input stream.
  (let [file_name (str filename)
        last-names (nlp.training/read-file nlp.training/last-name-file)
        first-names (nlp.training/read-file nlp.training/first-name-file)
        request-clauses (nlp.training/read-file nlp.training/request-clause-file)
        subject-clauses (nlp.training/read-file nlp.training/subject-clause-file)
        data {:data-lastnames last-names
              :data-firstname last-names
              :data-requests request-clauses
              :data-subjects subject-clauses}
        generators {:gen-datetime generate-datetime-tagged
                    :gen-duration nlp.training/generate-duration
                    :gen-subject  nlp.training/generate-subject
                    :gen-request  nlp.training/generate-request-clause}
        ]
    (with-open [wrt (io/writer file_name )]
      (doseq [sentence (take cnt
                             (repeatedly
                              #(nlp.training/generate-sentence data generators))) ]
        (.write wrt (str sentence "\n" )) ;; write line to file
        ))))


(defn train-datetime-model
  [training-filename output-filename]
  (let [datetime-finder-model (train/train-name-finder training-filename)]
    (nlp.training/store-model-file datetime-finder-model
                                   output-filename)
    ))

(defn create-datetime-model
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
   model to extract the Duration entity from each. The efficacy of the
   model is described by the success/total ratio."
  [sample-count]
  (let [
        datetime-find     (nlp/make-name-finder "models/en-event-datetime.bin")
        last-names        (nlp.training/read-file  nlp.training/last-name-file)
        first-names       (nlp.training/read-file  nlp.training/first-name-file)
        request-clauses   (nlp.training/read-file  nlp.training/request-clause-file)
        subject-clauses   (nlp.training/read-file  nlp.training/subject-clause-file)
        data {:data-lastnames last-names
              :data-firstname last-names
              :data-requests  request-clauses
              :data-subjects  subject-clauses}
        generators {:gen-datetime nlp.training/generate-datetime
                    :gen-duration nlp.training/generate-duration
                    :gen-subject  nlp.training/generate-subject
                    :gen-request  nlp.training/generate-request-clause}
        success   (reduce +
                           (take sample-count
                                 (repeatedly
                                  #(count (datetime-find
                                           (@nlp.core/tokenize
                                            (nlp.training/generate-sentence
                                                 data
                                                generators)))))))]
    (/ (float success) (float sample-count))
    )
  )

(ns nlp.training
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

(def last-name-file       "resources/last-names")
(def first-name-file      "resources/first-names")
(def request-clause-file  "resources/request-clauses")
(def subject-clause-file  "resources/subject-clauses")
(def custom-formatter     (ttf/formatter "MMMM d yyyy 'at' hh:mma"))

(defn read-file
 "Read entire content of file and split by lines.
  Returns array of lines."
 [file]
 (cljstr/split-lines (slurp file))
 )

(defn store-model-file
  "Store the binary model to disk for subsequent reuse."
  [bin-model model-file-name]
  (let [ out-stream (FileOutputStream. model-file-name)]
     (train/write-model bin-model out-stream)))

(defn generate-fullname
  [last-names first-names]
  (cljstr/join " " [(rand-nth first-names) (rand-nth last-names)])
  )

(defn generate-request-clause
  [request-clauses]
  (rand-nth request-clauses)
  )

(defn generate-participants-clause
  [a-fn a-ln]
  (let [participant-count (+ (rand-int 3) 1) ;; arbitrarily set to 1-3 participants
        seq-p (take participant-count (repeatedly #(generate-fullname a-fn a-ln)))
        participants (case participant-count
                       3 (str (first seq-p) ", " (nth seq-p 1) ", and " (nth seq-p 2))
                       2 (str (first seq-p) " and "  (second seq-p))
                       1 (first seq-p)
                       )
        ]
     (str "with " participants)))



(defn generate-datetime
  []
  (let [d (clj-time.core/plus
                              (clj-time.core/today-at 12 00)
                              (clj-time.core/days (rand-int 365))
                              (clj-time.core/hours (rand-int 23))
                              (clj-time.core/minutes (rand-nth [10 15 30 45 60])))]
     (ttf/unparse custom-formatter d)))



(defn generate-subject
  [subject-clauses]
  (rand-nth subject-clauses)
  )

(defn generate-duration
  "Generates the possible or likely specifications for the
   duration of a calendar event."
  []
  (let [ atime [1 2 3 4 5 6 7 8 9 10 15 30 45]
         time (rand-nth atime)
         unit   (cond
                    (== time 1) "hour"
                    (<= time 10) "hours"
                    (> time 10) "minutes"
                    )]
         (str (str time) " " unit)))

(defn generate-duration-tagged
  "Generates the possible or likely phrases for the duration
   of a calendar event with the appropriate tagging for
   training."
  []
  (str "<START:duration> " (generate-duration) " <END>")
  )

(defn rand-comma
  "Generate a random flip betwen a comma or space. This is
   used while generating training sentences."
  []
  (let [rnd (rand-int 2)]
    (cond
     (= rnd 0) " "
     (= rnd 1) ", "
     )))


(defn generate-space-padded-word
  "Generates a single space to be inserted into a
   sentence."
  ([] " ")
  ([word] (str " " word " "))
  )


(defn generate-sentence
  "Generates a single sentence with the following specification.
    [Request-Clause]
    [Participant-Clause]
    [DateTime-Clause]
    [Duration-Clause]
    [Subject-Clause].

   Where each of the clauses is generated randomly from a possible
   set of pre-defined values. An example:

      [Please schedule a meeting]
      [with Adam Smith and Sonya Smith]
      [on January 2016 at 1:30pm]
      [for 1 hour] [to discuss x, y and z].

   Parameters:
     [data] hash-map with keys:values
        data-firstnames:      array of first names
        data-lastnames:       array of last names
        data-requests:        array of requests
        data-subjects:        array of subjects

     [generators] hash-map with keys:values
       gen-datetime:         datetime generator function
       gen-duration:         duration generator function
       gen-subject:          subject generator function
       gen-request:          request generator function
   "
  [data generators ]

  (let [gen-datetime (:gen-datetime generators)
        gen-duration (:gen-duration generators)
        gen-subject (:gen-subject generators)
        gen-request (:gen-request generators)
        aln (:data-lastnames data)
        afn (:data-firstnames data)
        areq (:data-requests data)
        asub (:data-subjects data)
        ]

    (str (gen-request areq)
                           (generate-space-padded-word)
                           (generate-participants-clause aln afn)
                           (generate-space-padded-word "on")
                           (gen-datetime)
                           (generate-space-padded-word "for")
                           (gen-duration)
                           (generate-space-padded-word)
                           (gen-subject asub)
                           (generate-space-padded-word ".")
                           )

    ))



(defn generate-sentences-for-duration-training
  "Generate and writes [count] training sentences to [file],
   one per line."
  [cnt filename]
  ;; coerse the filename parameter to a string to avoid being
  ;; mistaken for input stream.
  (let [file_name (str filename)
        last-names (read-file last-name-file)
        first-names (read-file first-name-file)
        request-clauses (read-file request-clause-file)
        subject-clauses (read-file subject-clause-file)
        data {:data-lastnames last-names
              :data-firstname last-names
              :data-requests request-clauses
              :data-subjects subject-clauses}
        generators {:gen-datetime generate-datetime
                    :gen-duration generate-duration-tagged
                    :gen-subject generate-subject
                    :gen-request generate-request-clause}
        ]
    (with-open [wrt (io/writer file_name )]
      (doseq [sentence (take cnt
                             (repeatedly
                              #(generate-sentence data generators))) ]
        (.write wrt (str sentence "\n" )) ;; write line to file
        ))
    )
  )

(defn- train-duration-model
  "Trains a name-finder model using sentences that have been benerated by the
   (generate-sentences) form."
  [training-filename output-filename]
  (let [duration-finder-model (train/train-name-finder training-filename)]
      (store-model-file duration-finder-model output-filename)
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
        last-names (read-file last-name-file)
        first-names (read-file first-name-file)
        request-clauses (read-file request-clause-file)
        subject-clauses (read-file subject-clause-file)
        data {:data-lastnames last-names
              :data-firstname last-names
              :data-requests request-clauses
              :data-subjects subject-clauses}
        generators {:gen-datetime generate-datetime
                    :gen-duration generate-duration
                    :gen-subject generate-subject
                    :gen-request generate-request-clause}
        success   (reduce +
                           (take sample-count
                                 (repeatedly
                                  #(count (@nlp.core/duration-find
                                           (@nlp.core/tokenize
                                            (generate-sentence
                                                 data
                                                generators)))))))]
    (/ (float success) (float sample-count))
    )
  )

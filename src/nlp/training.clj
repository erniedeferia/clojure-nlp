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
(def custom-formatter     (ttf/formatter "MMM d yyyy 'at' hh:mma"))


(defn -read-file
 "Read entire content of file and split by lines.
  Returns array of lines."
 [file]
 (cljstr/split-lines (slurp file))
 )

(defn -store-model-file
  "Store the binary model to disk for subsequent reuse."
  [bin-model model-file-name]
  (let [ out-stream (FileOutputStream. model-file-name)]
     (train/write-model bin-model out-stream)))

(defn -generate-fullname
  [last-names first-names]
  (cljstr/join " " [(rand-nth first-names) (rand-nth last-names)])
  )

(defn -generate-request-clause
  [request-clauses]
  (rand-nth request-clauses)
  )

(defn -generate-participants-clause
  [a-fn a-ln]
  (let [participant-count (+ (rand-int 3) 1) ;; arbitrarily set to 1-3 participants
        seq-p (take participant-count (repeatedly #(-generate-fullname a-fn a-ln)))
        participants (case participant-count
                       3 (str (first seq-p) ", " (nth seq-p 1) ", and " (nth seq-p 2))
                       2 (str (first seq-p) " and "  (second seq-p))
                       1 (first seq-p)
                       )
        ]
     (str "with " participants)))


(defn -generate-datetime
  []
  (let [d (clj-time.core/plus (l/local-now)
                              (clj-time.core/months (rand-int 36))
                              (clj-time.core/weeks (rand-int 40))
                              (clj-time.core/hours (rand-int 23))
                              (clj-time.core/minutes (rand-int 59)))]
     (ttf/unparse custom-formatter d)))

(defn -generate-subject
  [subject-clauses]
  (rand-nth subject-clauses)
  )

(defn -generate-duration
  "Generates the possible or likely specifications for the
   duration of a calendar event."
  []
  (let [ atime [1 2 3 4 5 6 7 8 9 10 15 30 45]
         time (rand-nth atime)
         dim   (cond
                    (== time 1) "hour"
                    (<= time 10) "hours"
                    (> time 10) "minutes"
                    )]
         (str "for " (str time) " " dim)))

(defn -rand-comma
  "Generate a random flip betwen a comma or space. This is
   used while generating training sentences."
  []
  (let [rnd (rand-int 2)]
    (cond
     (= rnd 0) " "
     (= rnd 1) ", "
     )))

(defn -generate-sentence
  "Generates a single sentence with the following specification.
   [Request-Clause] [Participant-Clause] [DateTime-Clause] [Duration-Clause] [Subject-Clause].
   Where each of the clauses is generated randomly from a possible set of pre-defined values.
   An example: [Please schedule a meeting] [with Adam Smith and Sonya Smith] [on January 2016
   at 1:30pm] [for 1 hour] [to discuss x, y and z]."
  []
  (let [last-names (-read-file last-name-file)
        first-names (-read-file first-name-file)
        request-clauses (-read-file request-clause-file)
        subject-clauses (-read-file subject-clause-file)
        ]
    (str (-generate-request-clause request-clauses)
         " "
         (-generate-participants-clause last-names first-names)
         ", on "
         (-generate-datetime)
         (-rand-comma)
         " <START:duration> "
         (-generate-duration)
         " <END> "
         (-rand-comma)
         (-generate-subject subject-clauses)
         "."
         )
    )
  )

(defn generate-sentences
  "Generate and writes [count] training sentences to [file],
   one per line."
  [cnt filename]
  (let [file_name (str filename) ] ;; coerse the filename parameter to a string to avoid being mistaken for input stream.
    (with-open [wrt (io/writer file_name )]
      (doseq [sentence (take cnt (repeatedly #(-generate-sentence ))) ]
        (.write wrt (str sentence "\n" )) ;; write line to file
        ))
    )
  )

(defn train-duration-model
  "Trains a name-finder model using sentences that have been benerated by the
   (generate-sentences) form."
  [training-filename output-filename]
  (let [duration-finder-model (train/train-name-finder training-filename)]
      (-store-model-file duration-finder-model output-filename)
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
    (generate-sentences sentences-count sentences-filename)
    (train-duration-model sentences-filename output-filename)
    ))
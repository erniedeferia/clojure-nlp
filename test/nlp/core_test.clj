(ns nlp.core-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as tt :exclude [second]]
            [clj-time.format :as ttf :only [formatter unparse]]
            [clj-time.local :as l]
            [nlp.core :refer :all]))

(def standard-datetime-formatter (ttf/formatter
                                  "MMMM d yyyy 'at' h:mm a"
                                  (tt/default-time-zone)))



(deftest should-parse-message
  (testing "Meet with Monica Cooper and Anna Smith on July 4 2015 at 8:30AM for 2 hours to discuss XYZ - SHOULD PARSE CORRECTLY"
  (let [s "Meet with Monica Cooper and Anna Smith on July 4 2015 at 8:30AM for 2 hours to discuss XYZ."
        participants '("Monica Cooper" "Anna Smith")
        duration {:length "2" :time-unit "hours"}
        starts-at (ttf/unparse standard-datetime-formatter (l/to-local-date-time (tt/date-time 2015 7 4 8 30) )  )
        event-data (parse-message s)
        ]
    (is (= participants (:people event-data)))
    (is (= starts-at (ttf/unparse standard-datetime-formatter (:starts-at event-data) ) ))
    (is (= duration (:duration event-data)))
   )))

(deftest should-parse-message
  (testing "Meet with Monica Cooper, Anna Smith, and Robert Smith  on July 4 2015 at 8:30AM for 2 hours to discuss XYZ - SHOULD PARSE CORRECTLY"
  (let [s "Meet with Monica Cooper, Anna Smith, and Robert Smith  on July 4 2015 at 8:30AM for 2 hours to discuss XYZ."
        participants '("Monica Cooper" "Anna Smith" "Robert Smith")
        duration {:length "2" :time-unit "hours"}
        starts-at (ttf/unparse standard-datetime-formatter (l/to-local-date-time (tt/date-time 2015 7 4 8 30) )  )
        event-data (parse-message s)
        ]
    (is (= participants (:people event-data)))
    (is (= starts-at (ttf/unparse standard-datetime-formatter (:starts-at event-data) ) ))
    (is (= duration (:duration event-data)))
   )))

(deftest should-parse-message
  (testing "Meet with Monica Cooper, Anna Smith, and Robert Smith for 2 hours on July 4 2015 at 9:30AM to discuss XYZ - SHOULD PARSE CORRECTLY"
  (let [s "Meet with Monica Cooper, Anna Smith, and Robert Smith for 2 hours on July 4 2015 at 9:30AM to discuss XYZ."
        participants '("Monica Cooper" "Anna Smith" "Robert Smith")
        duration {:length "2" :time-unit "hours"}
        starts-at (ttf/unparse standard-datetime-formatter (l/to-local-date-time (tt/date-time 2015 7 4 9 30) )  )
        event-data (parse-message s)
        ]
    (is (= participants (:people event-data)))
    (is (= starts-at (ttf/unparse standard-datetime-formatter (:starts-at event-data) ) ))
    (is (= duration (:duration event-data)))
   )))

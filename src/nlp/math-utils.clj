(ns nlp.math-utils)

(defn select-randomly-between
  "Randomly selects either a or b."
  [a b]
  (rand-nth (list a b))
  )

(defn select-randomly-among
  "Randomly selects from a list (choices)."
  [choices]
  (rand-nth choices)
  )

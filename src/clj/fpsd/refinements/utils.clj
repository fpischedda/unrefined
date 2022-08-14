(ns fpsd.refinements.utils)

(defn utc-now
  []
  (java.time.LocalDateTime/now))

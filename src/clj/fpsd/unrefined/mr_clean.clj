(ns fpsd.unrefined.mr-clean
  (:require [fpsd.refinements.helpers :refer [utc-now]]
            [fpsd.refinements.state :as state]))

(defn filter-expired-refinements
  [refinements oldest-time]
  (into {}
        (filter (fn [[_ refinement]] (.isAfter (:updated-at refinement) oldest-time))
                refinements)))

(defn remove-old-refinements!
  [ttl]
  (state/transact!
   (fn [state]
     (update state :refinements filter-expired-refinements (.minusSeconds (utc-now) ttl)))))

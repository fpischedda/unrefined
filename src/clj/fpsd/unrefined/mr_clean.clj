(ns fpsd.unrefined.mr-clean
  (:require [manifold.stream :as s]
            [fpsd.refinements.helpers :refer [utc-now]]
            [fpsd.unrefined.state :as state]))

(defn clean-expired-refinements
  [state oldest-time]
  (reduce (fn [acc [ref-code refinement]]
            (if (.isAfter (:updated-at refinement) oldest-time)
              acc
              (do
                (s/close! (-> acc :refinements-sink (get ref-code)))
                (-> acc
                    (update :refinements dissoc ref-code)
                    (update :refinements-sink dissoc ref-code)))))
          state (:refinements state)))

(defn remove-old-refinements!
  [ttl]
  (state/transact!
   (fn [state]
     (clean-expired-refinements state (.minusSeconds (utc-now) ttl)))))

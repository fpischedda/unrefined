(ns fpsd.unrefined.mr-clean
  (:require [manifold.stream :as s]
            [fpsd.unrefined.state :as state]))

(defn all-downstream-drained?
  "Return true if all downstream streams (sinks) are
  drained."
  [stream]
  (every? (comp s/drained? second) (s/downstream stream)))

(defn clean-sources-with-drained-sinks
  "Return a new sources map removing all closed sources or
   sources with all drained sinks."
  [sources]
  (reduce (fn [acc [refinement-code source]]
            (if (or (s/closed? source) (all-downstream-drained? source))
              (do
                (s/close! source)
                acc)
              (assoc acc refinement-code source)))
          {} sources))

(defn remove-drained-sources!
  []
  (swap! state/state_ update :refinements-event-source clean-sources-with-drained-sinks))

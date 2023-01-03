(ns fpsd.unrefined.mr-clean
  (:require [manifold.stream :as s]
            [fpsd.unrefined.state :as state]))

(defn all-downstream-drained?
  [stream]
  (every? (comp s/drained? second) (s/downstream stream)))

(defn clean-sinks-with-drained-sources
  [sinks]
  (reduce (fn [acc [refinement-code sink]]
            (if (all-downstream-drained? sink)
              (do
                (s/close! sink)
                acc)
              (assoc acc refinement-code sink)))
          {} sinks))

(defn remove-drained-sinks!
  []
  (swap! state/state_ update :refinements-sink clean-sinks-with-drained-sources))

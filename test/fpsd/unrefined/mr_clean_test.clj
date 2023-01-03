(ns fpsd.unrefined.mr-clean-test
  (:require [clojure.test :refer [deftest is testing]]
            [manifold.stream :as s]
            [fpsd.refinements :as refinements]
            [fpsd.refinements.helpers :refer [utc-now]]
            [fpsd.unrefined.mr-clean :as mr-clean]
            [fpsd.unrefined.state :as state]))

(defn create-active-source
  []
  (let [[source sink1 sink2] (take 3 (repeatedly s/stream))
        _ (s/connect source sink1)
        _ (s/connect source sink2)
        _ (s/close! sink1)]
    source))

(defn create-drained-source
  []
  (let [[source sink1 sink2] (take 3 (repeatedly s/stream))
        _ (s/connect source sink1)
        _ (s/connect source sink2)
        _ (s/close! sink1)
        _ (s/close! sink2)]
    source))

(testing "Check if a source's downstream sinks are all drained"
  (deftest not-all-downstream-drained
    (is (not (mr-clean/all-downstream-drained? (create-active-source)))))

  (deftest all-downstream-drained
    (is (mr-clean/all-downstream-drained? (create-drained-source)))))

(testing "Remove refinements past ttl"
  (deftest filter-expired-refinements
    (is (= {}
           (mr-clean/clean-sinks-with-drained-sources {"REF1" (create-drained-source)}))))

  (deftest keep-non-expired-refinements
    (let [refinements {"REF-KEEP" (create-active-source)
                       "REF-REMOVE" (create-drained-source)}
          cleaned (mr-clean/clean-sinks-with-drained-sources refinements)]

      (is (and (some? (get cleaned "REF-KEEP"))
               (nil? (get cleaned "REF-REMOVED")))))))

(testing "Update the state removing refinements past ttl"
  (deftest stateful-filter-expired-refinements
    (let [_ (reset! state/state_ {:refinements-sink {"REF-KEEP" (create-active-source)
                                                     "REF-REMOVE" (create-drained-source)}})
          _ (mr-clean/remove-drained-sinks!)
          cleaned (:refinements-sink @state/state_)]
      (is (and (some? (get cleaned "REF-KEEP"))
               (nil? (get cleaned "REF-REMOVED")))))))

(ns fpsd.unrefined.mr-clean-test
  (:require [clojure.test :refer [deftest is testing]]
            [manifold.stream :as s]
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
           (mr-clean/clean-sources-with-drained-sinks {"REF1" (create-drained-source)}))))

  (deftest keep-non-expired-refinements
    (let [refinements {"REF-KEEP" (create-active-source)
                       "REF-REMOVE" (create-drained-source)}
          cleaned (mr-clean/clean-sources-with-drained-sinks refinements)]

      (is (and (some? (get cleaned "REF-KEEP"))
               (nil? (get cleaned "REF-REMOVED")))))))

(testing "Update the state removing refinements past ttl"
  (deftest stateful-filter-expired-refinements
    (let [_ (reset! state/state_ {:refinements-event-source {"REF-KEEP" (create-active-source)
                                                             "REF-REMOVE" (create-drained-source)}})
          _ (mr-clean/remove-drained-sources!)
          cleaned (:refinements-event-source @state/state_)]
      (is (and (some? (get cleaned "REF-KEEP"))
               (nil? (get cleaned "REF-REMOVED")))))))

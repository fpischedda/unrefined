(ns fpsd.unrefined.mr-clean-test
  (:require [clojure.test :refer [deftest is testing]]
            [manifold.stream :as s]
            [fpsd.refinements :as refinements]
            [fpsd.refinements.helpers :refer [utc-now]]
            [fpsd.unrefined.mr-clean :as mr-clean]
            [fpsd.unrefined.state :as state]))

(testing "Remove refinements past ttl"
  (deftest filter-expired-refinements
    (let [state {:refinements {"REF1" (refinements/create "REF1")}
                 :refinements-sink {"REF1" (s/stream)}}
          oldest-time (.plusSeconds (utc-now) 70)]
      (is (= {:refinements {}
              :refinements-sink {}}
             (mr-clean/clean-expired-refinements state oldest-time)))))

  (deftest keep-non-expired-refinements
    (let [state {:refinements {"REF1" (refinements/create "REF1")}
                 :refinements-sink {"REF1" "fake-stream"}}
          oldest-time (.minusSeconds (utc-now) 50)]
      (is (= state
             (mr-clean/clean-expired-refinements state oldest-time))))))

(testing "Update the state removing refinements past ttl"
  (deftest stateful-filter-expired-refinements
    (let [_ (state/transact! (fn [state] {:refinements {"REF1" (refinements/create "REF1")}
                                          :refinements-sink {"REF1" (s/stream)}}))
          _ (mr-clean/remove-old-refinements! -50)]
      (is (nil? (or (state/get-refinement "REF1") (state/get-refinement-sink "REF1"))))))

  (deftest stateful-keep-non-expired-refinements
    (let [_ (state/transact! (fn [state] {:refinements {"REF1" (refinements/create "REF1")}
                                          :refinements-sink {"REF1" "fake-stream"}}))
          _ (mr-clean/remove-old-refinements! 50)]
      (is (and (some? (state/get-refinement "REF1"))
               (some? (state/get-refinement-sink "REF1")))))))

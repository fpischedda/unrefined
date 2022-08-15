(ns fpsd.unrefined.mr-clean-test
  (:require [clojure.test :refer [deftest is testing]]
            [fpsd.refinements :as refinements]
            [fpsd.refinements.helpers :refer [utc-now]]
            [fpsd.unrefined.mr-clean :as mr-clean]
            [fpsd.unrefined.state :as state]))

(testing "Remove refinements past ttl"
  (deftest filter-expired-refinements
    (let [refinements {"REF1" (refinements/create "REF1")}
          oldest-time (.plusSeconds (utc-now) 70)]
      (is (= {}
             (mr-clean/filter-expired-refinements refinements oldest-time)))))

  (deftest keep-non-expired-refinements
    (let [refinements {"REF1" (refinements/create "REF1")}
          oldest-time (.minusSeconds (utc-now) 50)]
      (is (= refinements
             (mr-clean/filter-expired-refinements refinements oldest-time))))))

(testing "Update the state removing refinements past ttl"
  (deftest statefu-filter-expired-refinements
    (let [_ (state/transact! (fn [state] (assoc state :refinements {"REF1" (refinements/create "REF1")})))
          _ (mr-clean/remove-old-refinements! -50)]
      (is (nil? (state/get-refinement "REF1")))))

  (deftest stateful-keep-non-expired-refinements
    (let [_ (state/transact! (fn [state] (assoc state :refinements {"REF1" (refinements/create "REF1")})))
          _ (mr-clean/remove-old-refinements! 50)]
      (is (some? (state/get-refinement "REF1"))))))

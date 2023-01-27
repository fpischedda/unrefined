(ns fpsd.unrefined.persistence.datahike.migrator-test
    (:require
     [clojure.test :refer [deftest is testing]]
     [fpsd.refinements.helpers :refer [utc-now]]
     [fpsd.unrefined.persistence.datahike.migrator :as migrator]))

(def migrations
  [{:migration/name "test-01"
    :migrations/transaction [{:db/ident :some/ident}]}])

(def none-applied
  [])

(def all-applied
  [{:migration/name "test-01"
    :migration/applied-at (utc-now)
    :migrations/transaction "[{:db/ident :some/ident}]"}])

(def out-of-order
  [{:migration/name "test-00"
    :migration/applied-at (utc-now)
    :migrations/transaction "[{:db/ident :some/ident}]"}
   {:migration/name "test-01"
    :migration/applied-at (utc-now)
    :migrations/transaction "[{:db/ident :some/ident}]"}])

(deftest analyze-migrations
  (testing "Migration to be applied"
    (is (= (-> (migrator/analyze-migrations migrations none-applied)
               first
               :migration/status)
           :migration/not-applied)))

  (testing "Migration alredy applied"
    (is (= (-> (migrator/analyze-migrations migrations all-applied)
               first
               :migration/status)
           :migration/applied)))

  (testing "Migration out of order"
    (is (= (-> (migrator/analyze-migrations migrations out-of-order)
               first
               (select-keys [:migration/status :migration/error]))
           {:migration/status :migration/error
            :migration/error "Out of order migration, expected index 0, found 1"})))

  )

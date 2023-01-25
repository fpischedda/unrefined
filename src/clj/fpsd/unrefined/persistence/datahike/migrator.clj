(ns fpsd.unrefined.persistence.datahike.migrator
 "Utilities to apply schema changes to an existing database.
  So far only accretive changes are supported.

  The idea is to keep a list of applied migrations, compare it
  to what the application expects and run transactions with changes
  not yet applied to the db.

  At the same time we want to be sure that migrations recorded in the
  database are in sink with what the application axpects.

  Assume that the applications expects to have migrations a, b, c:
  - in the database we have recorded only a and c (missing migration)
  - in the database we have recorded a, c, b (out of order migrations)
  - in the database we have recorded a, b, b2, c (out of sync app, not covered yet)."
 (:require [datahike.api :as d]
           [fpsd.refinements.helpers :refer [utc-now]]))

(def migrations-schema
  [{:db/ident :migration/name
    :db/unique :db.unique/identity
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :migration/applied-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :migration/transactions
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(def initial-migration
  [{:migration/name "Initial migrator schema migration"
    :migration/transactions migrations-schema}])

(defn apply-migration
  [db name transactions]
  (let [migration {:migration/name name
                   :migration/applied-at (utc-now)
                   :migration/transactions (pr-str transactions)}]
    (d/transact db (conj transactions migration))
    migration))

(defn get-applied-migrations
  "Return a vector with all previously applied migrations,
   sorted by :migration/applied-at."
  [db]
  (->> db
       (d/q '[:find (pull ?e [*])
               :where [?e :migration/name]
                      [?e :migration/applied-at]
                      [?e :migration/transactions]])
       flatten
       (sort-by :migration/applied-at)))

(defn analyze-migrations
  "Return a vector of expected migrations compared to the current
   status recorded in the database (applied). Each element have a status
   according to previous state: :not-applied, :applied, :error."
  [expected applied]
  (let [applied-map
        (->> applied
             (map-indexed (fn [idx a] [(:migration/name a) (assoc a :migration/index idx)]))
             (into {}))]

    (->> expected
         (map-indexed
          (fn [idx e]
            (let [applied (get applied-map (:migration/name e))]
              (cond
                (nil? applied) (assoc e :migration/status :migration/not-applied)
                (= idx (:migration/index applied)) (assoc e :migration/status :migration/applied)
                :else
                (assoc e
                       :migration/status :migration/error
                       :migration/error (format "Out of order migration, expected index %s, found %s"
                                                idx (:migration/index applied)))))))
         (into []))))

(defn collect-migrations-to-apply
  "Return a map with migrations gruped by theirs status,
   :migration/applied, :migration/not-applied, :migration/error.
   The caller is expected to handle errors or apply not applied migrations"
  [db migrations]
  (->> db
       get-applied-migrations
       (analyze-migrations migrations)
       (group-by :migration/status)))

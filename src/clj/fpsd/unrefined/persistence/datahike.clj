(ns fpsd.unrefined.persistence.datahike
  (:require [datahike.api :as d]
            [mount.core :as mount]
            [nano-id.core :refer [nano-id]]
            [fpsd.configuration :refer [config]]
            [fpsd.refinements.helpers :refer [utc-now]]))

(def voting-mode-enum-schema
  [{:db/ident :voting.mode/linear}
   {:db/ident :voting.mode/poker}
   {:db/ident :voting.mode/t-shirt}])

(def refinement-schema
  [{:db/ident :refinement/id
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/created-at
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/updated-at
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/voting-mode
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/tickets
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}])

(def suggestion-strategy-enum-schema
  [{:db/ident :suggestion.strategy/majority}
   {:db/ident :suggestion.strategy/mean}])

(def voting-mode-linear-schema
  [{:db/ident :voting.mode.linear/refinement
    :db/valueType :db.type/ref
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :voting.mode.linear/max-points-delta
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :voting.mode.linear/minimum-votes
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :voting.mode.linear/max-rediscussions
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :voting.mode.linear/suggestion-strategy
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def ticket-schema
  [{:db/ident :ticket/refinement
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :ticket/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :ticket/refinement+id
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:ticket/refinement :ticket/id]
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :ticket/link-to-original
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :ticket/sessions
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   ])

(def estimation-session-status-enum-schema
  [{:db/ident :estimation.session.status/not-estimated}
   {:db/ident :estimation.session.status/estimated}
   {:db/ident :estimation.session.status/re-estimated}])

(def estimation-session-result-enum-schema
  [{:db/ident :estimation.session.result/winner}
   {:db/ident :estimation.session.result/discuss}
   {:db/ident :estimation.session.result/ex-equo}])

;; the composite ref is based on this
;; https://docs.datomic.com/cloud/schema/schema-reference.html#composite-tuples
;; it is useful to efficiently get a session by refinement+ticket and status
(def estimation-session-schema
  [{:db/ident :estimation-session/refinement
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}
   {:db/ident :estimation-session/ticket
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}
   {:db/ident :estimation-session/num
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long}

   {:db/ident :estimation-session/refinement+ticket+num
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:estimation-session/refinement
                    :estimation-session/ticket
                    :estimation-session/num]
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :estimation-session/votes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :estimation-session/status
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :estimation-session/result
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def estimation-schema
  [{:db/ident :estimation/session
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}
   {:db/ident :estimation/author-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :estimation/session+author
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:estimation/session
                    :estimation/author-id]
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :estimation/author-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :estimation/score
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}])

(defn full-schema
  []
  (concat voting-mode-enum-schema
          refinement-schema
          suggestion-strategy-enum-schema
          voting-mode-linear-schema
          ticket-schema
          estimation-session-status-enum-schema
          estimation-session-result-enum-schema
          estimation-session-schema
          estimation-schema))

(mount/defstate db
  :start (do
           ;; create a database at this place, per default configuration
           ;; we enforce a strict schema and keep all historical data
           (d/create-database (-> config
                                  :datahike
                                  (assoc :initial-tx (full-schema))))
           (d/connect (:datahike config)))
  :stop (d/delete-database (:datahike config)))

(comment
  (mount/start)
  (mount/stop)
  ,)

(comment
  ;; given a refinement id
  (do
    (def _refinement (nano-id))
    (def _ticket-id "asdf"))

  ;; add some data
  (do
    ;; lets create a refinement session
    (d/transact db
                [{:refinement/id _refinement
                  :refinement/created-at (str (utc-now))
                  :refinement/updated-at (str (utc-now))
                  :refinement/voting-mode :voting.mode/linear}])

    ;; adding some settings to it
    (d/transact db
                [{:voting.mode.linear/refinement [:refinement/id _refinement]
                  :voting.mode.linear/max-points-delta 3
                  :voting.mode.linear/minimum-votes 3
                  :voting.mode.linear/max-rediscussions 1
                  :voting.mode.linear/suggestion-strategy :suggestion.strategy/majority}])

    ;; now add a ticket to the refinement session
    (d/transact db
                [{:refinement/_tickets [:refinement/id _refinement]
                  :ticket/refinement _refinement
                  :ticket/id _ticket-id
                  :ticket/link-to-original "asdf"}])

    ;; to estimate a ticket we start with a non estimated session
    (d/transact db
                [{:ticket/_sessions [:ticket/refinement+id [_refinement _ticket-id]]
                  :estimation-session/refinement _refinement
                  :estimation-session/ticket _ticket-id
                  :estimation-session/num 0
                  :estimation-session/status :estimation.session.status/not-estimated
                  }])

    ;; and then we add some votes to it
    (d/transact db
                [{:estimation-session/_votes [:estimation-session/refinement+ticket+num [_refinement _ticket-id 0]]
                  :estimation/session [:estimation-session/refinement+ticket+num [_refinement _ticket-id 0]]
                  :estimation/author-id "person-1"
                  :estimation/author-name "Bob"
                  :estimation/score 4}])

    ,)


  ;; queries
  (d/pull @db '[*] [:refinement/id _refinement])

  (d/pull @db
          '[* {:refinement/_tickets [*]
               :ticket/sessions [*]}]
          [:ticket/refinement+id [_refinement _ticket-id]])

  (d/pull @db
          '[*]
          [:voting-session/refinement+ticket+num [_refinement _ticket-id 0]])


  (d/pull @db
          '[* {:refinement/tickets [*]
               :refinement/voting-mode [*]}]
          [:refinement/id _refinement])

  (d/pull @db
          '[* {:refinement/_tickets [*]}]
          [:ticket/refinement+id [_refinement _ticket-id]])

  (d/pull @db
          '[* {:voting-session/votes [*]}]
          [:voting-session/refinement+ticket+num [_refinement _ticket-id 0]])

  ,)

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

(def voting-session-status-enum-schema
  [{:db/ident :voting.session.status/not-estimated}
   {:db/ident :voting.session.status/estimated}
   {:db/ident :voting.session.status/re-estimated}])

(def voting-session-result-enum-schema
  [{:db/ident :voting.session.result/winner}
   {:db/ident :voting.session.result/discuss}
   {:db/ident :voting.session.result/ex-equo}])

;; the composite ref is based on this
;; https://docs.datomic.com/cloud/schema/schema-reference.html#composite-tuples
;; it is useful to efficiently get a session by refinement+ticket and status
(def voting-session-schema
  [{:db/ident :voting-session/refinement+ticket
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}
   {:db/ident :voting-session/num
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long}

   {:db/ident :voting-session/refinement+ticket+num
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:voting-session/refinement+ticket
                    :voting-session/num]
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :voting-session/votes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :voting-session/status
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :voting-session/result
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def ticket-vote-schema
  [{:db/ident :ticket-vote/author-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :ticket-vote/author-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :ticket-vote/score
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}])

(defn full-schema
  []
  (concat voting-mode-enum-schema
          refinement-schema
          suggestion-strategy-enum-schema
          voting-mode-linear-schema
          ticket-schema
          voting-session-result-enum-schema
          voting-session-schema
          ticket-vote-schema))

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

  ;; lets create a refinement session
  (d/transact db
              [{:refinement/id _refinement
                :refinement/created-at (str (utc-now))
                :refinement/updated-at (str (utc-now))
                :refinement/voting-mode :voting.mode/linear}])

  (d/pull @db '[*] [:refinement/id _refinement])

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

  (d/pull @db
          '[* {:refinement/tickets [*]
               :refinement/voting-mode [*]}]
          [:refinement/id _refinement])

  (d/pull @db
          '[* {:refinement/_tickets [*]}]
          [:ticket/refinement+id [_refinement _ticket-id]])

  (d/pull @db
          '[* {:ticket/refinement [*]}]
          [:ticket/refinement+id [33 _ticket-id]])
  ;; to estimate a ticket we start with a non estimated session
  (d/transact db
              [{:voting-session/ticket [:refinement/id _refinement
                                        :ticket/id _ticket-id]
                :voting-session/num 0
                :voting-session/status :voting.session.status/not-estimated}])

  ;; and then we add some votes to it
  (d/transact db
              [{:voting-session/_votes [:ticket/id _ticket-id]
                :ticket-vote/author-id "person-1"
                :ticket-vote/author-name "Bob"
                :ticket-vote/score 4}])

  (d/transact db
              [{:voting-session/_votes [:ticket/id _ticket-id]
                :ticket-vote/author-id "person-2"
                :ticket-vote/author-name "Alice"
                :ticket-vote/score 4}])

  ,)

(ns one.unrefined.persistence.datahike.schema)

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
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/updated-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/voting-mode
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/settings
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :refinement/tickets
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}])

(def suggestion-strategy-enum-schema
  [{:db/ident :suggestion.strategy/majority}
   {:db/ident :suggestion.strategy/mean}])

(def voting-mode-linear-schema
  [{:db/ident :voting.mode.linear/max-points-delta
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
    :db/cardinality :db.cardinality/one}
   {:db/ident :voting.mode.linear/cheatsheet
    :db/valueType :db.type/string
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
;; it is useful to efficiently get a session by refinement+ticket and num
;; which is the "index" of re-estimations
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
    :db/cardinality :db.cardinality/one}
   {:db/ident :estimation/skipped?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}])


(def estimation-breakdown-schema
  [;; breakdown related properties
   {:db/ident :estimation-breakdown/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :estimation-breakdown/points
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   ;; estimation reference to breakdowns
   ;; it is not in the original estimation schema
   ;; because it was introduced later
   {:db/ident :estimation/breakdown
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}])

(def cheatsheet
  [{:db/ident :cheatsheet/uuid
    :db/doc "External id to easily work with cheatsheet entities"
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :cheatsheet/name
    :db/doc "Display name of the cheatsheet"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :cheatsheet/breakdown
    :db/doc "List of breakdown entities with their name and points"
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}])

(def cheatsheet-breakdown
  [{:db/ident :cheatsheet-breakdown/name
    :db/doc "The name of the breakdown item"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :cheatsheet-breakdown/uuid+name
    :db/doc "A cheatsheet breakdown entity must be unique only in the context of a cheatsheet"
    :db/valueType :db.type/tuple
    :db/tupleAttrs [:cheatsheet/uuid
                    :cheatsheet-breakdown/name]
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :cheatsheet-breakdown/description
    :db/doc "A brief description of the breakdown entry"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :cheatsheet-breakdown/items
    :db/doc "Breakdown items are basically a list of descriptions"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}])

(defn full-schema
  "full-schema represents, well, the full schema of the application,
   to be applied when creating a database from scratch.
   On the other end to apply changes to an existing schema, migrations
   must be used. Migrations started with the estimation breakdown."
  []
  (concat voting-mode-enum-schema
          refinement-schema
          suggestion-strategy-enum-schema
          voting-mode-linear-schema
          ticket-schema
          estimation-session-status-enum-schema
          estimation-session-result-enum-schema
          estimation-session-schema
          estimation-schema
          estimation-breakdown-schema
          cheatsheet
          cheatsheet-breakdown))

(def migrations
  [{:migration/name "add estimation breakdown"
    :migration/transactions estimation-breakdown-schema}
   #_{:migration/name "add cheatsheet"
    :migration/transactions (concat cheatsheet cheatsheet-breakdown)}])

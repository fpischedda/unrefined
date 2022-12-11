(ns fpsd.unrefined.persistence.datahike
  (:require [datahike.api :as d]
            [mount.core :as mount]
            [nano-id.core :refer [nano-id]]
            [fpsd.configuration :refer [config]]
            [fpsd.unrefined.persistence.datahike.schema :as schema]
            [fpsd.refinements.helpers :refer [utc-now]]))

(mount/defstate db
  :start (do
           ;; create a database at this place, per default configuration
           ;; we enforce a strict schema and keep all historical data
           (d/create-database (-> config
                                  :datahike
                                  (assoc :initial-tx (schema/full-schema))))
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
    (d/transact db
                [
    ;; lets create a refinement session
                 {:refinement/id _refinement
                  :refinement/created-at (str (utc-now))
                  :refinement/updated-at (str (utc-now))
                  :refinement/voting-mode :voting.mode/linear}
    ;; adding some settings to it
                 {:voting.mode.linear/refinement [:refinement/id _refinement]
                  :voting.mode.linear/max-points-delta 3
                  :voting.mode.linear/minimum-votes 3
                  :voting.mode.linear/max-rediscussions 1
                  :voting.mode.linear/suggestion-strategy :suggestion.strategy/majority}
    ;; now add a ticket to the refinement session
                 {:refinement/_tickets [:refinement/id _refinement]
                  :ticket/refinement _refinement
                  :ticket/id _ticket-id
                  :ticket/link-to-original "asdf"}

    ;; to estimate a ticket we start with a non estimated session
                 {:ticket/_sessions [:ticket/refinement+id [_refinement _ticket-id]]
                  :estimation-session/refinement _refinement
                  :estimation-session/ticket _ticket-id
                  :estimation-session/num 0
                  :estimation-session/status :estimation.session.status/not-estimated
                  }
                 ])

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

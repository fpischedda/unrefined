(ns fpsd.unrefined.persistence.datahike
  (:require
   [com.brunobonacci.mulog :as u]
   [datahike.api :as d]
   [mount.core :as mount]
   [nano-id.core :refer [nano-id]]
   [fpsd.configuration :refer [config]]
   [fpsd.unrefined.persistence.datahike.migrator :as migrator]
   [fpsd.unrefined.persistence.datahike.schema :as schema]
   [fpsd.refinements.helpers :refer [utc-now]]))

(defn migrate-schema!
  "Try to apply migrations and log errors if any."
  [db migrations]
  (let [{:migration/keys [error applied not-applied]}
        (migrator/collect-migrations-to-apply db migrations)]
    (if error
      (u/log ::migrate-schema
             :errors error
             :message "Unable to migrate the schema")
      (do
        (u/log ::migrate-schema
               :migrations-to-apply not-applied
               :migrations-applied applied
               :message "Applying migrations")
        (doseq [{:migration/keys [name transactions]} not-applied]
          (migrator/apply-migration db name transactions))))))

(mount/defstate db
  :start (do
           ;; create a database at this place, per default configuration
           ;; we enforce a strict schema and keep all historical data
           (try
             (d/create-database (-> config
                                    :datahike
                                    (assoc :initial-tx (schema/full-schema))))
             (catch Throwable t
               (u/log ::create-database
                      :config (:datahike config)
                      :message "Unable to create DB"
                      :exception t)))
           (let [db (d/connect (:datahike config))]
             (migrate-schema! db (concat migrator/initial-migration schema/migrations))
             db))
  :stop (d/release db))

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
                 {:refinement/_settings [:refinement/id _refinement]
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
                  :estimation/score 4
                  :estimation/skipped? false}])

    ,)

  ;; queries
  (d/pull @db '[*] [:refinement/id _refinement])

  (d/pull @db
          '[* {:refinement/_tickets [*]
               :ticket/sessions [* {:estimation-session/votes [*]}]}]
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

  ;; trying the migrator manually :)
  (d/transact db migrator/migrations-schema)

  (migrator/get-applied-migrations db)
  
  (migrator/analyze-migrations (concat migrator/initial-migration schema/migrations)
                               (migrator/get-applied-migrations db))

  (migrator/collect-migrations-to-apply db schema/migrations)
  
  (migrate-schema! db schema/migrations)
  ,)

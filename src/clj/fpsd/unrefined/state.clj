(ns fpsd.unrefined.state
  (:require [datahike.api :as d]
            [fpsd.unrefined.persistence.datahike :refer [db]]))

(defn insert-refinement
  [refinement cheatsheet]
  (let [code (:code refinement)]
    (d/transact
     db
     [
      ;; lets create a refinement session
      {:refinement/id code
       :refinement/created-at (:created-at refinement)
       :refinement/updated-at (:updated-at refinement)
       :refinement/voting-mode :voting.mode/linear}

      ;; adding some settings to it
      {:refinement/_settings [:refinement/id code]
       :voting.mode.linear/max-points-delta 3
       :voting.mode.linear/minimum-votes 3
       :voting.mode.linear/max-rediscussions 1
       :voting.mode.linear/suggestion-strategy :suggestion.strategy/majority
       :voting.mode.linear/cheatsheet cheatsheet}
      ])))

(defn insert-ticket
  [code ticket]

  (let [tid (:id ticket)]
    (d/transact
     db
     [
      ;; add a ticket to the refinement session
      {:refinement/_tickets [:refinement/id code]
       :ticket/refinement code
       :ticket/id tid
       :ticket/link-to-original (:link-to-original ticket)}

      ;; to estimate a ticket we start with a non estimated session
      {:ticket/_sessions [:ticket/refinement+id [code tid]]
       :estimation-session/refinement code
       :estimation-session/ticket tid
       :estimation-session/num 0
       :estimation-session/status :estimation.session.status/not-estimated}
      ])))

(defn new-estimation-session
  [code ticket]

  (let [tid (:id ticket)
        {cur-session-id :db/id cur-session-num :num} (-> ticket :sessions last)]
    (d/transact
     db
     [
      ;; set last session status to :re-estimated
      {:db/id cur-session-id
       :estimation-session/status :estimation.session.status/re-estimated}

      ;; create a new estiamtion session
      {:ticket/_sessions [:ticket/refinement+id [code tid]]
       :estimation-session/refinement code
       :estimation-session/ticket tid
       :estimation-session/num (inc cur-session-num)
       :estimation-session/status :estimation.session.status/not-estimated}
      ])))

(defmulti db->voting-mode-settings (fn [voting-mode _settings] voting-mode))

(defmethod db->voting-mode-settings :voting.mode/linear
  [voting-mode
   {:voting.mode.linear/keys [max-points-delta
                              minimum-votes
                              max-rediscussions
                              suggestion-strategy
                              cheatsheet]
    :or {cheatsheet "default"}
    :as _settings}]
  {:voting-mode voting-mode
   :max-points-delta max-points-delta
   :minimum-votes minimum-votes
   :max-rediscussions max-rediscussions
   :suggestion-strategy suggestion-strategy
   :estimation-cheatsheet cheatsheet})

(defn db->refinement
  [{:refinement/keys [id created-at updated-at voting-mode settings] :as refinement}]
  {:db/id (:db/id refinement)
   :code id
   :created-at created-at
   :updated-at updated-at
   :settings (db->voting-mode-settings (:db/ident voting-mode) settings)})

(defn db->estimation-breakdown
  [{:estimation-breakdown/keys [name points]}]
  {(keyword name) points})

(defn db->estimation
  [{:estimation/keys [author-id author-name score skipped? breakdown] :as estimation}]
  {:db/id (:db/id estimation)
   :author-id author-id
   :author-name author-name
   :score score
   :skipped? skipped?
   :breakdown (->> breakdown (mapv db->estimation-breakdown) (into {}))})

(defn db->session
  [{:estimation-session/keys [num votes status result] :as session}]
  {:db/id (:db/id session)
   :num num
   :status (:db/ident status)
   :result (:db/ident result)
   :votes (mapv db->estimation votes)})

(defn get-current-session
  [sessions]
  (let [session (last sessions)
        votes (for [{:keys [author-id author-name score skipped?]} (:votes session)
                    :when (not skipped?)]
                {:user-id author-id
                 :name author-name
                 :points score})
        skips (for [{:keys [author-id skipped?]} (:votes session)
                    :when skipped?]
                author-id)]
    (assoc session
           :votes votes
           :skips skips)))

(defn db->ticket
  [{:ticket/keys [id link-to-original sessions] :as ticket}]
  (let [sessions (mapv db->session sessions)]
    {:db/id (:db/id ticket)
     :id id
     :link-to-original link-to-original
     :sessions sessions
     :current-session (get-current-session sessions)}))

(defn get-refinement-ticket
  [code ticket-id]
  (let [res (d/pull @db
                    '[* {:refinement/_tickets [* {:refinement/voting-mode [:db/ident]
                                                  :refinement/settings [*]}]
                         :ticket/sessions [* {:estimation-session/status [:db/ident]
                                              :estimation-session/votes [* {:estimation/breakdown [:estimation-breakdown/name
                                                                                                   :estimation-breakdown/points]}]}]}]
                    [:ticket/refinement+id [code ticket-id]])]
    {:refinement (db->refinement (-> res :refinement/_tickets first))
     :ticket (db->ticket res)}))

(defn get-ticket
  [code ticket-id]
  (let [res (d/pull @db
                    '[* {:ticket/sessions [* {:estimation-session/status [:db/ident]
                                              :estimation-session/votes [*]}]}]
                    [:ticket/refinement+id [code ticket-id]])]
    {:ticket (db->ticket res)}))

(comment
  (get-refinement-ticket  "Lw5h_kM8FYWHq4gM59H2x" "asdf")

  (d/pull @db
          '[* {:refinement/_tickets [* {:refinement/voting-mode [*]
                                        :refinement/settings [*]}]
               :ticket/sessions [* {:estimation-session/votes [*]}]}]
          [:ticket/refinement+id ["Hyu7AnkjyM9vY5InBZKCX" "asdf"]])

  )

(defn add-estimation
  [refinement ticket-id session-num
   {:keys [author-id author-name score skipped? breakdown] :as _estimation}]
  (let [session-id [:estimation-session/refinement+ticket+num [refinement ticket-id session-num]]]
    (d/transact db
                 [{:estimation-session/_votes session-id
                   :estimation/session session-id
                   :estimation/author-id author-id
                   :estimation/author-name author-name
                   :estimation/score score
                   :estimation/skipped? skipped?
                   :estimation/breakdown (for [[kw-name points] breakdown]
                                           {:estimation-breakdown/name (name kw-name)
                                            :estimation-breakdown/points points})}])))

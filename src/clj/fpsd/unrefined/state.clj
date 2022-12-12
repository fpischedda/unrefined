(ns fpsd.unrefined.state
  (:require [datahike.api :as d]
            [fpsd.unrefined.persistence.datahike :refer [db]]
            [fpsd.refinements.helpers :refer [utc-now]]))

(def state_ (atom {:refinements {}
                   :refinements-sink {}
                   :last-updates {}}))

(comment
  (:refinements @state_)
  (-> @state_ :refinements (get "SDYFFD") :tickets (get "asdf"))
  (-> @state_ :refinements-sink (get "MJXHBI"))
  (get-refinement-sink "MJXHBI")

  ,)

(defn get-by-code_
  [key code]
  (-> state_
      deref
      key
      (get code)))

(defn get-refinement
  [code]
  (get-by-code_ :refinements code))

(defn get-refinements
  []
  (:refinements @state_))

(defn get-refinement-sink
  [code]
  (get-by-code_ :refinements-sink code))

(defn transact!
  ([update-fn]
   (swap! state_ update-fn))
  ([update-fn arg]
   (swap! state_ update-fn arg))
  ([update-fn arg1 arg2]
   (swap! state_ update-fn arg1 arg2))
  ([update-fn arg1 arg2 arg3]
   (swap! state_ update-fn arg1 arg2 arg3))
  ([update-fn arg1 arg2 arg3 arg4]
   (swap! state_ update-fn arg1 arg2 arg3 arg4))
  ([update-fn arg1 arg2 arg3 arg4 arg5]
   (swap! state_ update-fn arg1 arg2 arg3 arg4 arg5)))


(defn insert-refinement
  [refinement]
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
      {:voting.mode.linear/refinement [:refinement/id code]
       :voting.mode.linear/max-points-delta 3
       :voting.mode.linear/minimum-votes 3
       :voting.mode.linear/max-rediscussions 1
       :voting.mode.linear/suggestion-strategy :suggestion.strategy/majority}
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

(defmulti db->voting-mode-settings (fn [voting-mode _settings] voting-mode))

(defmethod db->voting-mode-settings :voting.mode/linear
  [voting-mode
   {:voting.mode.linear/keys [max-points-delta minimum-votes max-rediscussions suggestion-strategy]
    :as settings}]
  {:voting-mode voting-mode
   :max-points-delta max-points-delta
   :minimum-votes minimum-votes
   :max-rediscussions max-rediscussions
   :suggestion-strategy suggestion-strategy})

(defn db->refinement
  [{:refinement/keys [id created-at updated-at voting-mode settings] :as refinement}]
  {:code id
   :created-at created-at
   :updated-at updated-at
   :settings (db->voting-mode-settings (:db/ident voting-mode) settings)})

(defn db->estimation
  [{:estimation/keys [author-id author-name score] :as estimation}]
  {:author-id author-id
   :author-name author-name
   :score score})

(defn db->session
  [{:estimation-session/keys [num votes status result] :as session}]
  {:num num
   :status (:db/ident status)
   :result (:db/ident result)
   :votes (mapv db->estimation votes)})

(defn db->ticket
  [{:ticket/keys [id link-to-original sessions] :as ticket}]
  {:id id
   :link-to-original link-to-original
   :sessions (mapv db->session sessions)})

(defn get-refinement-ticket
  [code ticket-id]
  (let [res (d/pull @db
                    '[* {:refinement/_tickets [* {:refinement/voting-mode [:db/ident]
                                                  :refinement/settings [*]}]
                         :ticket/sessions [* {:estimation-session/status [:db/ident]
                                              :estimation-session/votes [*]}]}]
                    [:ticket/refinement+id [code ticket-id]])]
    {:refinement (db->refinement (-> res :refinement/_tickets first))
     :ticket (db->ticket res)}))

(comment
  (get-refinement-ticket  "Lw5h_kM8FYWHq4gM59H2x" "asdf")

  (d/pull @db
          '[* {:refinement/_tickets [* {:refinement/voting-mode [*]
                                        :refinement/settings [*]}]
               :ticket/sessions [* {:estimation-session/votes [*]}]}]
          [:ticket/refinement+id ["Lw5h_kM8FYWHq4gM59H2x" "asdf"]])

  )

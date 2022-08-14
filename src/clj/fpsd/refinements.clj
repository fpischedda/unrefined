(ns fpsd.refinements
  (:require [fpsd.estimator :as estimator]
            [fpsd.refinements.helpers :refer [utc-now]]))

(def default-settings {:max-points-delta 3
                       :reasonable-minimum-votes 3
                       :max-rediscussions 1
                       :suggestion-strategy :majority})

(defn ticket-details
  [refinement ticket-id]
  (-> refinement :tickets (get ticket-id)))

;; refinements code
(defn create
  "Return a new refinement with its own unique code and the provided
   settings; the newly created refinement is atomically added to the
   global map of active refinements."
  ([code]
   (create code {}))
  ([code settings]
   (let [now (utc-now)]
     {:code code
      :settings (merge default-settings settings)
      :tickets {}
      :created-at now
      :updated-at now})))

(defn new-empty-session
  []
  {:votes {}
   :skips #{}})

(defn new-ticket
  [ticket-id ticket-url]
  {:id ticket-id
   :current-session (new-empty-session)
   :sessions []
   :link-to-original ticket-url})

(defn add-ticket
  [refinement ticket]
  (-> refinement
      (update :tickets assoc (:id ticket) ticket)))

(defn add-new-ticket
  [refinement ticket-id ticket-url]
  (add-ticket refinement (new-ticket ticket-id ticket-url)))

(defn count-voted
  [ticket]
  (count (-> ticket :current-session :votes)))

(defn count-skipped
  [ticket]
  (count (-> ticket :current-session :skips)))

(defn vote-ticket
  "Store that a user voted and send an event accordingly"
  [refinement ticket-id user-id vote]
  (update-in refinement [:tickets ticket-id :current-session]
             (fn [session]
               (-> session
                   (update :skips disj user-id)
                   (update :votes assoc user-id vote)))))

(defn skip-ticket
  "Store that a user skipped voting and send an event accordingly"
  [refinement ticket-id user-id]
  (update-in refinement [:tickets ticket-id :current-session]
             (fn [session]
               (-> session
                   (update :skips conj user-id)
                   (update :votes dissoc user-id)))))

(defn new-estimation-for-ticket
  "Given a ticket, move the current estimation to the :sessions list
   and set :current-session to an empty session.
   Returns the updated ticket."
  [ticket]
  (-> ticket
      (update :sessions conj (:current-session ticket))
      (assoc :current-session (new-empty-session))))

(defn re-estimate-ticket
  "Updates the ticket to start a new estimation"
  [refinement ticket-id]
  (update-in refinement [:tickets ticket-id] new-estimation-for-ticket))

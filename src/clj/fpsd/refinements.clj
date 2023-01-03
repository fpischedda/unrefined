(ns fpsd.refinements
  (:require [fpsd.estimator :as estimator]
            [fpsd.refinements.helpers :refer [utc-now]]))

(def default-settings {:max-points-delta 3
                       :minimum-votes 3
                       :max-rediscussions 1
                       :suggestion-strategy :majority
                       :cheatsheet "cargo-one"})

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
      :created-at now
      :updated-at now})))

(defn new-ticket
  [ticket-id ticket-url]
  {:id ticket-id
   :link-to-original ticket-url})

(defn count-voted
  "Returns the numbers of users who voted"
  [ticket]
  (count (-> ticket :current-session :votes)))

(defn count-skipped
  "Returns the numbers of users who skipped voting"
  [ticket]
  (count (-> ticket :current-session :skips)))

(defn new-estimation-for-ticket
  "Given a ticket, return a new ticket where the current estimation is
   moved to the :sessions list and set :current-session to an empty session."
  [ticket]
  (-> ticket
      (update :sessions conj (:current-session ticket))
      (assoc :current-session {})))

(defn re-estimate-ticket
  "Return an updated refinement starting a new session for the
   ticket identified by ticket-id"
  [refinement ticket-id]
  (update-in refinement [:tickets ticket-id] new-estimation-for-ticket))

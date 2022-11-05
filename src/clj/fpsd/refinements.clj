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
  "Returns a new refinement adding the provided ticket to the tickets map"
  [refinement ticket]
  (update refinement :tickets assoc (:id ticket) ticket))

(defn add-new-ticket
  [refinement ticket-id ticket-url]
  (add-ticket refinement (new-ticket ticket-id ticket-url)))

(defn count-voted
  "Returns the numbers of users who voted"
  [ticket]
  (count (-> ticket :current-session :votes)))

(defn count-skipped
  "Returns the numbers of users who skipped voting"
  [ticket]
  (count (-> ticket :current-session :skips)))

(defn vote-ticket
  "Return un updated refinement adding or changing a user's vote for a ticket"
  [refinement ticket-id user-id vote]
  (update-in refinement [:tickets ticket-id :current-session]
             (fn [session]
               (-> session
                   (update :skips disj user-id)
                   (update :votes assoc user-id vote)))))

(defn skip-ticket
  "Return un updated refinement adding or changing a user's skip for a ticket"
  [refinement ticket-id user-id]
  (update-in refinement [:tickets ticket-id :current-session]
             (fn [session]
               (-> session
                   (update :skips conj user-id)
                   (update :votes dissoc user-id)))))

(defn new-estimation-for-ticket
  "Given a ticket, return a new ticket where the current estimation is
   moved to the :sessions list and set :current-session to an empty session."
  [ticket]
  (-> ticket
      (update :sessions conj (:current-session ticket))
      (assoc :current-session (new-empty-session))))

(defn re-estimate-ticket
  "Return an updated refinement starting a new session for the
   ticket identified by ticket-id"
  [refinement ticket-id]
  (update-in refinement [:tickets ticket-id] new-estimation-for-ticket))

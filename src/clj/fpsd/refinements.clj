(ns fpsd.refinements
  (:require [fpsd.estimator :as estimator]))

(def default-settings {:max-points-delta 3
                       :reasonable-minimum-votes 3
                       :max-rediscussions 1
                       :suggestion-strategy :majority})

(def refinements_ (atom {}))

(defn details
  [code]
  (get @refinements_ code))

(defn ticket-details
  [code ticket-id]
  (-> code details :tickets (get ticket-id)))

(defn gen-random-code [length]
  (loop []
    (let [code (apply str (take length (repeatedly #(char (+ (rand 26) 65)))))]
      (if (get @refinements_ code)
        (recur)
        code))))

;; refinements code
(defn create!
  "Return a new refinement with its own unique code and the provided
   settings; the newly created refinement is atomically added to the
   global map of active refinements."
  ([]
   (create! {}))
  ([settings]
   (let [code (gen-random-code 6)
         refinement {:code code
                     :settings (merge default-settings settings)
                     :tickets {}}]
     (swap! refinements_ assoc code refinement)
     refinement)))

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

(defn add-ticket!
  [code ticket]
  (swap! refinements_ update-in [code :tickets] assoc (:id ticket) ticket)
  ticket)

(defn add-new-ticket!
  [code ticket-id ticket-url]
  (add-ticket! code (new-ticket ticket-id ticket-url)))

(defn count-voted
  [ticket]
  (count (-> ticket :current-session :votes)))

(defn count-skipped
  [ticket]
  (count (-> ticket :current-session :skips)))

(defn vote-ticket
  "Store that a user voted and send an event accordingly"
  [code ticket-id user-id vote]
  (swap! refinements_
         update-in [code :tickets ticket-id :current-session]
         (fn [session]
           (-> session
               (update :skips disj user-id)
               (update :votes assoc user-id vote)))))

(defn skip-ticket
  "Store that a user skipped voting and send an event accordingly"
  [code ticket-id user-id]
  (swap! refinements_
         update-in [code :tickets ticket-id :current-session]
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
  "Updates the ticket to start a new estimation and send an event to clients"
  [code ticket-id]
  (swap! refinements_ update-in [code :tickets ticket-id]
         new-estimation-for-ticket))

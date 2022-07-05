(ns fpsd.refinements
  (:require [cheshire.core :refer [generate-string]]
            [manifold.stream :as s]))

(def default-settings {:max-vote-delta 3
                       :voting-style :linear ;; or :fibonacci
                       :max-rediscussions 1
                       :suggestion-strategy :majority})

(def refinements_ (atom {}))

(identity @refinements_)
(identity (get @refinements_ "OKCAVG"))

(defn details
  [code]
  (get @refinements_ code))

(defn gen-random-code [length]
  (loop []
    (let [code (apply str (take length (repeatedly #(char (+ (rand 26) 65)))))]
      (if (get @refinements_ code)
        (recur)
        code))))

;; SSE related code
(defn user-connected
  "Every refinement session have a event-sink (a stream in manifold, chan in core.async);
   once users land to a refinement page a stream is created to send them events,
   this stream is connected to the event-sync so, every message sent to it will be
   dispatched to user streams."
  [code user-id]
  (let [user-stream (s/stream)
        {event-sink :event-sink} (details code)]
    (s/connect event-sink user-stream)
    user-stream))

(defn serialize-event
  "Return a one line string with the event serialized to JSON,
  plus a new-line character."
  [event]
  (str "data: " (generate-string event) "\n\n"))

(defn send-event!
  "Given a refinement identified by code, send a SSE event to all users
   connected to the page.
   Returns the result of s/put! but this is subject to change so use it
   at your own risk."
  [code event]
  (let [serialized (serialize-event event)
        {event-sink :event-sink} (details code)]
    (s/put! event-sink serialized)))

;; refinements code
(defn create!
  "Return a new refinement with its own unique code, the provided
   settings, the owner name and id; the newly created refinement
   is atomically added to the global list of active refinements."
  [owner-id settings]
  (let [code (gen-random-code 6)
        refinement {:code code
                    :settings (merge default-settings settings)
                    :tickets []
                    :owner owner-id
                    :participants {}
                    :event-sink (s/stream)}]
    (swap! refinements_ assoc code refinement)
    refinement))

(defn is-owner
  "Return true if the user-id is the owner of the refinement"
  [refinement user-id]
  (= user-id (:owner refinement)))

(defn add-ticket
  [code ticket-id]
  (let [ticket {:id ticket-id
                :status :unrefined
                :score nil
                :sessions []}]
    (swap! refinements_ update-in [code :tickets] conj ticket)
    ticket))

(defn add-participant
  [code user-id name]
  (swap! refinements_ update-in [code :participants] assoc user-id {:name name :voter true}))

(comment
  (add-ticket "OKCAVG" "PE-1234")
  (identity (get @refinements_ "OKCAVG"))
  ,)

(defn vote-ticket
  [code ticket-id user-id vote]
  (swap! refinements_
         update-in [code ticket-id :sessions last]
         (fn [session]
           (-> session
               (update :skips disj user-id)
               (update session :votes assoc user-id vote))))
  (send-event! code {:event :user-voted
                     :payload {:user-id user-id
                               :ticket-id ticket-id}}))

(defn skip-ticket
  [code ticket-id user-id]
  (swap! refinements_
         update-in [code ticket-id :sessions last]
         (fn [session]
           (-> session
               (update :skips conj user-id)
               (update :votes dissoc user-id))))
  (send-event! code {:event :user-skipped
                     :payload {:user-id user-id
                               :ticket-id ticket-id}}))

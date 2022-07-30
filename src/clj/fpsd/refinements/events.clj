(ns fpsd.refinements.events
  (:require [cheshire.core :refer [generate-string]]
            [manifold.stream :as s]
            [fpsd.estimator :as estimator]
            [fpsd.refinements :as refinements]))

(def refinement-sinks_ (atom {}))

(defn create-refinement-sink!
  [code]
  (swap! refinement-sinks_ assoc code (s/stream)))

(defn get-sink
  [code]
  (get @refinement-sinks_ code))

(defn user-connected!
  "Every refinement session have an event-sink (a stream in manifold,
   or chan in core.async);
   once users land to a refinement page a stream is created to send them events,
   this stream is connected to the event-synk so, every message sent to it will
   be dispatched to user streams."
  [code]
  (let [user-stream (s/stream)
        event-sink (get-sink code)]
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
        {event-sink :event-sink} (get-sink code)]
    (s/put! event-sink serialized)))

(defn send-ticket-added-event!
  "Send an event to signal that a new ticket is being estimated"
  [code ticket-id]
  (send-event! code {:event :added-ticket
                     :payload {:code code
                               :ticket_id ticket-id}}))

(defn send-vote-event!
  "Send a vote event (both for voting and skipping) with some stats about the
   current voting state of the ticket."
  [event code user-id ticket]
  (send-event! code {:event event
                     :payload {:user-id user-id
                               :voted (refinements/count-voted ticket)
                               :skipped (refinements/count-skipped ticket)
                               :ticket-id (:id ticket)
                               :votes (-> ticket :current-session :votes estimator/count-votes)}}))

(defn send-ticket-status-event!
  "Send an event with the current status of the ticket,
   usually sent when a user connects for the first time, to refresh
   ticket stats."
  [code ticket]
  (send-event! code {:event :ticket-status
                     :payload {:voted (refinements/count-voted ticket)
                               :skipped (refinements/count-skipped ticket)
                               :ticket-id (:id ticket)
                               :votes (-> ticket :current-session :votes estimator/count-votes)}}))

(defn send-re-estimate-event!
  "Send an event to signal that a new estimation for a ticket is starting"
  [code ticket-id]
  (send-event! code {:event :re-estimate-ticket
                     :payload {:code code
                               :ticket_id ticket-id}}))

(ns fpsd.refinements.events
  (:require [cheshire.core :refer [generate-string]]
            [manifold.bus :as b]
            [one.unrefined.estimator :as estimator]
            [fpsd.refinements :as refinements]))

(defonce event-bus (b/event-bus))

(defn user-connected!
  "Every refinement session have a specific topic on the event-bus, each user connecting
   to receive events will subscribe to that topic and will get back a stream."
  [topic]
  (b/subscribe event-bus topic))

(defn serialize-event
  "Return a one line string with the event serialized to JSON,
  plus a new-line character."
  [event]
  (str "data: " (generate-string event) "\n\n"))

(defn send-event!
  "Given a refinement topic, send a SSE event to all users connected to the page.
   Returns the result of b/publish! but this is subject to change so use it
   at your own risk. It could be handy if to know if all subscribers received
   the message."
  [topic event]
  (b/publish! event-bus topic (serialize-event event)))

(defn send-ticket-added-event!
  "Send an event to signal that a new ticket is being estimated"
  [topic code ticket-id]
  (send-event! topic {:event :added-ticket
                      :payload {:code code
                                :ticket_id ticket-id}}))

(defn send-vote-event!
  "Send a vote event (both for voting and skipping) with some stats about the
   current voting state of the ticket."
  [topic event user-id ticket]
  (send-event! topic {:event event
                      :payload {:user-id user-id
                                :voted (refinements/count-voted ticket)
                                :skipped (refinements/count-skipped ticket)
                                :ticket-id (:id ticket)
                                :votes (-> ticket :current-session :votes estimator/count-votes)}}))

(defn send-ticket-status-event!
  "Send an event with the current status of the ticket,
   usually sent when a user connects for the first time, to refresh
   ticket stats."
  [topic ticket]
  (send-event! topic {:event :ticket-status
                      :payload {:voted (refinements/count-voted ticket)
                                :skipped (refinements/count-skipped ticket)
                                :ticket-id (:id ticket)
                                :votes (-> ticket :current-session :votes estimator/count-votes)}}))

(defn send-re-estimate-event!
  "Send an event to signal that a new estimation for a ticket is starting"
  [topic code ticket-id]
  (send-event! topic {:event :re-estimate-ticket
                      :payload {:code code
                                :ticket_id ticket-id}}))

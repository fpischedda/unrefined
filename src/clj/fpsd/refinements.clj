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

;; sse related code
(defn user-connected
  [code user-id]
  (let [user-stream (s/stream)
        {event-sink :event-sink} (details code)]
    (s/connect event-sink user-stream)
    user-stream))

(defn serialize-event
  "Return a one line string with the event serialized to JSON,
  plus a new-line character"
  [{:keys [event payload] :as _event}]
  (str "event: " event "\ndata: " (generate-string payload) "\n\n"))

(defn send-event
  [code event]
  (let [serialized (serialize-event event)
        {event-sink :event-sink} (details code)]
    (s/put! event-sink serialized)))

;; refinements code
(defn create
  [settings owner-id name]
  (let [code (gen-random-code 6)
        refinement {:code code
                    :settings (merge default-settings settings)
                    :tickets []
                    :owner owner-id
                    :participants {owner-id {:name name
                                             :owner true}}
                    :event-sink (s/stream)}]
    (swap! refinements_ assoc code refinement)
    refinement))

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
         update-in [code ticket-id :sessions last :votes]
         assoc user-id vote)
  (send-event code {:event :user-voted
                    :payload {:user-id user-id
                              :ticket-id ticket-id}}))

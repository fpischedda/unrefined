(ns fpsd.refinements.core
  (:require [nano-id.core :refer [nano-id]]
            [fpsd.configuration :refer [config]]
            [fpsd.estimator :as estimator]
            [fpsd.refinements.events :as events]
            [fpsd.refinements :as refinements]
            [fpsd.refinements.helpers :refer [utc-now] :as helpers]
            [fpsd.unrefined.persistence :as persistence]
            [fpsd.unrefined.persistence.json-file]
            [fpsd.unrefined.state :as state]
            [fpsd.unrefined.ticket-parser :refer [fetch-jira-ticket]]))

(defn get-refinement
  [code]
  (state/get-refinement code))

(defn get-refinement-ticket
  [refinement ticket-id]
  (-> refinement
      :tickets
      (get ticket-id)))

(defn get-ticket
  [code ticket-id]
  (-> (state/get-refinement code)
      :tickets
      (get ticket-id)))

(defn user-connected!
  [code ticket-id]
  (let [sink (state/get-refinement-sink code)
        stream (events/user-connected! sink)]
    (events/send-event! sink {:event "ping"})
    (events/send-ticket-status-event! sink
                                      (-> code
                                          state/get-refinement
                                          (refinements/ticket-details ticket-id)))
    stream))

(defn gen-random-code
  "Generate a random code/id using nano-id lib.
   By default (no-params) returns a code of length 21
   but smaller codes can be generated with a higher
   chances of collision."
  ([]
   (gen-random-code 21))
  ([length]
   (nano-id length)))

(defn create-refinement
  [ticket-url]

  (let [ticket-id (or (helpers/extract-ticket-id-from-url ticket-url)
                      ticket-url)
        code (gen-random-code)]
    (state/transact!
     (fn [state]
       (let [refinement (-> code
                            refinements/create
                            (refinements/add-new-ticket ticket-id ticket-url))]
         (-> state
             (update :refinements assoc code refinement)
             (update :refinements-sink assoc code (events/new-stream))))))

    (state/insert-refinement (state/get-refinement code))
    (state/insert-ticket code (-> (state/get-refinement code) :tickets first))

    {:code code
     :ticket-id ticket-id}))

(defn add-ticket
  [code ticket-url]
  (let [ticket-id (or (helpers/extract-ticket-id-from-url ticket-url)
                      ticket-url)]

    (state/transact!
     (fn [state]
       (-> state
           (update-in [:refinements code] refinements/add-new-ticket ticket-id ticket-url)
           (update-in [:refinemets code] assoc :updated-at (utc-now)))))

    (state/insert-ticket code (-> (state/get-refinement code) :tickets (get ticket-id)))

    (events/send-ticket-added-event! (state/get-refinement-sink code) code ticket-id)

    (get-ticket code ticket-id)))

(defn vote-ticket
  [code ticket-id user-id skipped vote]
  (state/transact!
   (fn [state]
     (if skipped
       (-> state
           (update-in [:refinements code]
                      refinements/skip-ticket ticket-id user-id)
           (update-in [:refinements code] assoc :updated-at (utc-now)))
       (-> state
           (update-in [:refinements code]
                      refinements/vote-ticket ticket-id user-id vote)
           (update-in [:refinements code] assoc :updated-at (utc-now))))))

  (if skipped
    (events/send-vote-event! (state/get-refinement-sink code)
                             :user-skipped user-id ticket-id)
    (events/send-vote-event! (state/get-refinement-sink code)
                             :user-voted user-id ticket-id)))

(defn re-estimate-ticket
  [code ticket-id]
  (state/transact! update-in [:refinements code]
                   (fn [refinement]
                     (-> refinement
                         (refinements/re-estimate-ticket ticket-id)
                         (assoc :updated-at (utc-now)))))

  (events/send-re-estimate-event! (state/get-refinement-sink code)
                                  code ticket-id))

(defn store-ticket
  [code ticket-id]
  (let [refinement (get-refinement code)
        ticket (get-ticket code ticket-id)]
    (cond
      (nil? refinement) (format "Unable to find refinement %s" code)
      (nil? ticket) (format "Unable to find ticket %s in refinement %s" ticket-id code)
      :else
      (persistence/store-ticket! (:persistence config)
       code
       ticket-id
       {:refinement (dissoc refinement :tickets)
        :ticket ticket
        :estimation (estimator/estimate ticket (:settings refinement))}))))

(defn get-stored-ticket
  [code ticket-id]
  (try
    (persistence/get-stored-ticket (:persistence config) code ticket-id)
    (catch Throwable t
      {:error (format "Unable to retrieve ticket %s for refinement %s: %s" ticket-id code t)})))

(defn ticket-preview
  [code ticket-id]
  (state/transact! update-in [:refinements code :tickets ticket-id]
                   (fn [ticket]
                     (if-not (:preview ticket)
                       (assoc ticket :preview (delay (fetch-jira-ticket ticket-id)))
                       ticket)))
  (:preview (get-ticket code ticket-id)))

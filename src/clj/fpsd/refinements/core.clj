(ns fpsd.refinements.core
  (:require [fpsd.estimator :as estimator]
            [fpsd.refinements.events :as events]
            [fpsd.refinements :as refinements]
            [fpsd.refinements.helpers :refer [utc-now] :as helpers]
            [fpsd.unrefined.persistence :as persistence]
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

(defn gen-random-code [length existing-refinements]
  (loop []
    (let [code (apply str (take length (repeatedly #(char (+ (rand 26) 65)))))]
      (if (get existing-refinements code)
        (recur)
        code))))

(defn create-refinement
  [ticket-url]

  (let [ticket-id (or (helpers/extract-ticket-id-from-url ticket-url)
                      ticket-url)
        code (gen-random-code 6 (state/get-refinements))]
    (state/transact!
     (fn [state]
       (let [refinement (-> code
                            refinements/create
                            (refinements/add-new-ticket ticket-id ticket-url))]
         (-> state
             (update :refinements assoc code refinement)
             (update :refinements-sink assoc code (events/new-stream))))))

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
      :else (persistence/store-ticket-to-file! refinement
                                               ticket
                                               (estimator/estimate ticket (:settings refinement))))))

(defn get-stored-ticket
  [code ticket-id]
  (try
    (persistence/get-stored-ticket code ticket-id)
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

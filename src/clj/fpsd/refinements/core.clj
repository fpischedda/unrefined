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
            [fpsd.unrefined.ticket-parser :refer [fetch-jira-ticket]]
            [com.brunobonacci.mulog :as u]))

(defn get-refinement-ticket
  "Return a map with :refinement and :ticket keys holding
  respectively the refinement and ticket data.
  On error return a map with a :error key holding a string of
  the error."
  [code ticket-id]
  (try
    (state/get-refinement-ticket code ticket-id)

    (catch clojure.lang.ExceptionInfo e
      (u/log ::get-refinement-ticket
             :message "Unable to fetch ticket or refinement"
             :error (ex-data e))
      {:error "Unable to find ticket or refinement session"})))

(defn get-ticket
  "Return a map with  :ticket key holding the ticket data.
  On error return a map with a :error key holding a string of
  the error."
  [code ticket-id]
  (try
    (state/get-ticket code ticket-id)
    (catch clojure.lang.ExceptionInfo e
      (let [error-msg (format "Unable to fetch ticket %s" ticket-id)]
        (u/log ::get-ticket
               :message error-msg
               :error (ex-data e))
        {:error error-msg}))))

(defn user-connected!
  [code ticket-id]
  (let [sink (state/get-or-create-refinement-sink code)
        stream (events/user-connected! sink)
        {:keys [ticket error]} (get-ticket code ticket-id)]

    (events/send-event! sink {:event "ping"})

    (if ticket
      ;; send last status of ticket as message
      (events/send-ticket-status-event! sink ticket)

      ;; otherwise send and error message
      (events/send-ticket-status-event! sink {:error error}))

    ;; and return the new event stream
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
  "Return a new randomly generated refinement code and the ticket-id extracted
  from ticket-url (if possible, otherwise defaults to ticket-url).
  As a side effect the refinement and ticket data are stored in the
  application state."
  [ticket-url]

  (let [ticket-id (or (helpers/extract-ticket-id-from-url ticket-url)
                      ticket-url)
        code (gen-random-code)
        refinement (refinements/create code)
        ticket (refinements/new-ticket ticket-id ticket-url)]

    (state/insert-refinement refinement "default")
    (state/insert-ticket code ticket)

    {:code code
     :ticket-id ticket-id}))

(defn add-ticket
  [code ticket-url]
  (let [ticket-id (or (helpers/extract-ticket-id-from-url ticket-url)
                      ticket-url)
        ticket (refinements/new-ticket ticket-id ticket-url)]

    (state/insert-ticket code ticket)

    (events/send-ticket-added-event! (state/get-or-create-refinement-sink code) code ticket-id)

    ticket))

(defn vote-ticket
  [{:keys [code ticket-id session-num author-id author-name vote] :as _estimation}]

  (state/add-estimation code ticket-id session-num
                        {:author-id author-id
                         :author-name author-name
                         :score (long (:points vote))
                         :skipped? (boolean (:skipped? vote))})

  (events/send-vote-event! (state/get-or-create-refinement-sink code)
                           (if (:skipped? vote) :user-skipped :user-voted)
                           author-id
                           (state/get-ticket code ticket-id)))


(defn re-estimate-ticket
  [code ticket-id]
  ;; to be implemented using datahike
  #_(state/transact! update-in [:refinements code]
                   (fn [refinement]
                     (-> refinement
                         (refinements/re-estimate-ticket ticket-id)
                         (assoc :updated-at (utc-now)))))

  (events/send-re-estimate-event! (state/get-or-create-refinement-sink code)
                                  code ticket-id))

(defn store-ticket
  "To be deprecated eventually"
  [code ticket-id]
  (let [{:keys [refinement ticket error]} (get-refinement-ticket code ticket-id)]

    (cond
      (nil? refinement) (format "Unable to find refinement %s, error %s" code error)
      (nil? ticket) (format "Unable to find ticket %s in refinement %s, error %s" ticket-id code error)
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

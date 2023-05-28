(ns fpsd.refinements.core
  (:require [nano-id.core :refer [nano-id]]
            [fpsd.refinements.events :as events]
            [fpsd.refinements :as refinements]
            [fpsd.refinements.helpers :as helpers]
            [fpsd.unrefined.state :as state]
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
  "Return a stream for the subscription to refinement events topic.
   The refinement code is used as the topic name.
   If not ticket is found do not subscribe and return an error."
  [code ticket-id]
  (let [{:keys [ticket error]} (get-ticket code ticket-id)]
    (if ticket
      (let [stream (events/user-connected! code)]
        ;; send last status of ticket as message
        (events/send-ticket-status-event! code ticket)
        {:stream stream}) ;; return the stream for the subscription
      {:error error})))

(defn gen-random-code
  "Generate a random code/id using nano-id lib.
   By default (no-params) returns a code of length 21
   but smaller codes can be generated with a higher
   chances of collision."
  ([]
   (gen-random-code 21))
  ([length]
   (nano-id length)))

(defn create-refinement!
  "Return a new randomly generated refinement code and the ticket-id extracted
  from ticket-url (if possible, otherwise defaults to ticket-url).
  As a side effect the refinement and ticket data are stored in the
  application state."
  [ticket-url settings]

  (let [ticket-id (or (helpers/extract-ticket-id-from-url ticket-url)
                      ticket-url)
        code (gen-random-code)
        refinement (refinements/create code settings)
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

    (events/send-ticket-added-event! code code ticket-id)

    ticket))

(defn estimate-ticket
  [{:keys [code ticket-id session-num author-id author-name estimation] :as _estimation}]

  (state/add-estimation code ticket-id session-num
                        {:author-id author-id
                         :author-name author-name
                         :score (:score estimation)
                         :skipped? (boolean (:skipped? estimation))
                         :breakdown (:breakdown estimation)})

  (events/send-vote-event! code
                           (if (:skipped? estimation) :user-skipped :user-voted)
                           author-id
                           (state/get-ticket code ticket-id)))


(defn re-estimate-ticket
  [code ticket]
  (state/new-estimation-session code ticket)

  (events/send-re-estimate-event! code code (:id ticket)))

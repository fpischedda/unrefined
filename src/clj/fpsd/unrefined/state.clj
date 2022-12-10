(ns fpsd.unrefined.state
  (:require [datahike.api :as d]
            [fpsd.unrefined.persistence.datahike :refer [db]]
            [fpsd.refinements.helpers :refer [utc-now]]))

(def state_ (atom {:refinements {}
                   :refinements-sink {}
                   :last-updates {}}))

(comment
  (:refinements @state_)
  (-> @state_ :refinements (get "SDYFFD") :tickets (get "asdf"))
  (-> @state_ :refinements-sink (get "MJXHBI"))
  (get-refinement-sink "MJXHBI")

  ,)

(defn get-by-code_
  [key code]
  (-> state_
      deref
      key
      (get code)))

(defn get-refinement
  [code]
  (get-by-code_ :refinements code))

(defn get-refinements
  []
  (:refinements @state_))

(defn get-refinement-sink
  [code]
  (get-by-code_ :refinements-sink code))

(defn transact!
  ([update-fn]
   (swap! state_ update-fn))
  ([update-fn arg]
   (swap! state_ update-fn arg))
  ([update-fn arg1 arg2]
   (swap! state_ update-fn arg1 arg2))
  ([update-fn arg1 arg2 arg3]
   (swap! state_ update-fn arg1 arg2 arg3))
  ([update-fn arg1 arg2 arg3 arg4]
   (swap! state_ update-fn arg1 arg2 arg3 arg4))
  ([update-fn arg1 arg2 arg3 arg4 arg5]
   (swap! state_ update-fn arg1 arg2 arg3 arg4 arg5)))


(defn insert-refinement
  [refinement]
  (let [code (:code refinement)]
    (d/transact
     db
     [
      ;; lets create a refinement session
      {:refinement/id code
       :refinement/created-at (:created-at refinement)
       :refinement/updated-at (:updated-at refinement)
       :refinement/voting-mode :voting.mode/linear}

      ;; adding some settings to it
      {:voting.mode.linear/refinement [:refinement/id code]
       :voting.mode.linear/max-points-delta 3
       :voting.mode.linear/minimum-votes 3
       :voting.mode.linear/max-rediscussions 1
       :voting.mode.linear/suggestion-strategy :suggestion.strategy/majority}
      ])))

(defn insert-ticket
  [code ticket]

  (let [tid (:id ticket)]
    (d/transact
     db
     [
      ;; add a ticket to the refinement session
      {:refinement/_tickets [:refinement/id code]
       :ticket/refinement code
       :ticket/id tid
       :ticket/link-to-original (:link-to-original ticket)}

      ;; to estimate a ticket we start with a non estimated session
      {:ticket/_sessions [:ticket/refinement+id [code tid]]
       :estimation-session/refinement code
       :estimation-session/ticket tid
       :estimation-session/num 0
       :estimation-session/status :estimation.session.status/not-estimated}
      ])))

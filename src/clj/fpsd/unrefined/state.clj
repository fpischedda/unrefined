(ns fpsd.unrefined.state)

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

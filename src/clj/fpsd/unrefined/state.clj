(ns fpsd.unrefined.state)

(def state_ (atom {:refinements {}
                   :refinements-sink {}
                   :last-updates {}}))

(comment
  (:refinements @state_)
  (-> @state_ :refinements (get "ZPUAKO"))
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
  [update-fn]
  (swap! state_ update-fn))

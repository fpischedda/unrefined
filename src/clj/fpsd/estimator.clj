(ns fpsd.estimator)

(def settings {:max-points-delta 3
               :voting-style :linear ;; or :fibonacci
               :max-rediscussions 1
               :suggestion-strategy :majority})

(def ticket {:id "PE-12345"
             :story-points nil
             :sessions [{:story-points nil ;; the final story points of the session
                         :points {"id1" {:points 2 :name "Bob"}
                                  "id2" {:points 3 :name "Alice"}}}]})

(defn count-votes
  [votes]
  (->> (group-by (comp :points second) votes)
       (reduce (fn [acc [points point-and-authors]]
                 (conj acc {:points points
                            :count (count point-and-authors)
                            :authors (mapv (comp :name second) point-and-authors)}))
               [])
       (sort-by :points)
       (reverse)
       (into [])))

(defn votes-delta
  "Returns the difference between the maximum and minumum value in the
  provided sorted seq of vote frequencies; an entry looks like:
  [vote [voters...]]"
  [frequencies]
  (- (-> frequencies first (:points 0)) (-> frequencies last (:points 0))))

(defn select-winner
  [counted-votes]
  (let [sorted (reverse (sort-by :count counted-votes))
        [{first-vote :points first-voters :authors}
         {second-vote :points second-voters :authors} _] sorted]
    (println sorted)
    (if (= (count first-voters) (count second-voters))
      {:result :ex-equo
       :suggested (max first-vote second-vote)
       :same-points [{:points first-vote :authors first-voters}
                     {:points second-vote :authors second-voters}]}
      {:result :winner
       :points first-vote})))

(defn can-rediscuss
  [sessions settings]
  (<= (count sessions) (:max-rediscussions settings)))

(comment
  (can-rediscuss (-> ticket :sessions) settings) ;; true
  (can-rediscuss (-> ticket :sessions) (update settings :max-rediscussions dec)) ;;false
 ,)

(defn estimate
  [{:keys [current-session sessions] :as _ticket} settings]
  (let [votes (-> current-session :votes count-votes)
        delta (votes-delta votes)
        result
        (if (or (< delta (:max-points-delta settings))
                (not (can-rediscuss sessions settings)))
          (do
            (println votes)
            (select-winner votes))

          (let [{lowest-vote :points lowest-voters :authors} (last votes)
                {highest-vote :points highest-voters :authors} (first votes)]
            {:result :discuss
             :highest-vote highest-vote
             :highest-voters highest-voters
             :lowest-vote lowest-vote
             :lowest-voters lowest-voters}))]
    (-> result
        (assoc :votes votes))))

(comment

  (estimate {:current-session {:story-points nil
                               :votes {"1" {:points 0 :name "Bob"}
                                        "2" {:points 1 :name "Alice"}
                                        "3" {:points 4 :name "Joe"}
                                        "4" {:points 3 :name "Foo"}}}
             :sessions []}
            settings) ;; {:result :discuss, :highest-vote 4, :highest-voters ["Joe"], :lowest-vote 0, :lowest-voters ["Bob"], :votes [{:points 4, :count 1, :authors ["Joe"]} {:points 3, :count 1, :authors ["Foo"]} {:points 1, :count 1, :authors ["Alice"]} {:points 0, :count 1, :authors ["Bob"]}]}

  )

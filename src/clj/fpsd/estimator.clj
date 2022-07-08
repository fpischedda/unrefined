(ns fpsd.estimator)

(def settings {:max-vote-delta 3
               :voting-style :linear ;; or :fibonacci
               :max-rediscussions 1
               :suggestion-strategy :majority})

(def ticket {:id "PE-12345"
             :story-points nil
             :sessions [{:story-points nil ;; the final story points of the session
                         :events [] ;; possibly hold a list of events like, voted, skipped, session started/stopped
                         :votes {"Francesco" 2
                                 "Luke" 2}}]})

(defn count-votes
  [votes]
  (->> (group-by second votes)
       (reduce (fn [acc [vote authors]]
                 (conj acc {:vote vote
                            :count (count authors)
                            :authors (mapv first authors)}))
               [])
       (sort-by :vote)
       (reverse)
       (into [])))

(comment

  (count-votes {"Luke" 3
                "Yaroslav" 2
                "Emmanuel" 2
                "Fra" 1
                "Stan" 3
                "Nikhil" 5}) ;; => [{:vote 5, :count 1, :authors ["Nikhil"]} {:vote 3, :count 2, :authors ["Luke" "Stan"]} {:vote 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:vote 1, :count 1, :authors ["Fra"]}]
  )

(defn votes-delta
  "Returns the difference between the maximum and minumum value in the
  provided sorted seq of vote frequencies; an entry looks like:
  [vote [voters...]]"
  [frequencies]
  (- (-> frequencies first :vote) (-> frequencies last :vote)))

(comment
  (votes-delta [{:vote 5, :count 1, :authors ["Nikhil"]} {:vote 3, :count 2, :authors ["Luke" "Stan"]} {:vote 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:vote 1, :count 1, :authors ["Fra"]}] ) ;; => 4
  ,)

(defn select-winner
  [counted-votes]
  (let [sorted (reverse (sort-by :count counted-votes))
        [{first-vote :vote first-voters :authors}
         {second-vote :vote second-voters :authors} _] sorted]
    (if (= (count first-voters) (count second-voters))
      {:result :ex-equo
       :suggested (max first-vote second-vote)
       :same-votes [[first-vote first-voters] [second-vote second-voters]]}
      {:result :winner
       :vote first-vote})))

(comment
  (select-winner [{:vote 5, :count 1, :authors ["Nikhil"]} {:vote 3, :count 2, :authors ["Luke" "Stan"]} {:vote 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:vote 1, :count 1, :authors ["Fra"]}]) ;; => {:result :ex-equo, :suggested 3, :same-votes [2 3]}

  (select-winner [{:vote 5, :count 1, :authors ["Nikhil"]} {:vote 3, :count 2, :authors ["Luke" "Stan"]} {:vote 2, :count 3, :authors ["Yaroslav" "Emmanuel" "Julio"]} {:vote 1, :count 1, :authors ["Fra"]}]) ;; => {:result :winner, :vote 2}
  ,)

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
        (if (or (< delta (:max-vote-delta settings))
                (not (can-rediscuss sessions settings)))
          (select-winner votes)

          (let [{lowest-vote :vote lowest-voters :authors} (last votes)
                {highest-vote :vote highest-voters :authors} (first votes)]
            {:result :discuss
             :highest-vote highest-vote
             :highest-voters highest-voters
             :lowest-vote lowest-vote
             :lowest-voters lowest-voters}))]
    (-> result
        (assoc :votes votes))))

(comment

  (estimate ticket settings) ;; {:result :winner, :vote 2}

  (estimate {:current-session {:story-points nil
                               :votes {"Yaroslav" 3
                                       "Luke" 2
                                       "Francesco" 2
                                       "Julio" 3}}
             :sessions []}
            settings) ;; => {:result :ex-equo, :suggested 3, :votes [2 3]}

  (estimate {:current-session {:story-points nil
                               :votes {"Stan" 0
                                       "Luke" 2
                                       "Francesco" 4
                                       "Julio" 3}}
             :sessions []}
            settings) ;; => {:result :discuss, :highest-vote 4, :highest-voters ["Francesco"], :lowest-vote 0, :lowest-voters ["Stan"]}

  (estimate {:current-session {:story-points nil
                               :votes {"Stan" 3
                                       "Luke" 2
                                       "Francesco" 4
                                       "Julio" 3}}
             :sessions [{:story-points nil
                         :votes {"Stan" 4
                                 "Luke" 2
                                 "Francesco" 3
                                 "Julio" 0}}]}
            settings) ;; => {:result :winner, :vote 3}
  )

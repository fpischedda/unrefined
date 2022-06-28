(ns fpsd.estimator)

(def settings {:max-vote-delta 3
               :voting-style :linear ;; or :fibonacci
               :max-rediscussions 1
               :suggestion-strategy :majority})

(def ticket {:identifier "PE-12345"
             :story-points nil
             :sessions [{:story-points nil
                         :votes [{:author "Francesco"
                                  :vote 2}
                                 {:author "Luke"
                                  :vote 2}]}]})

(defn count-votes
  [votes]
  (->> (group-by :vote votes)
       (reduce (fn [acc [vote authors]]
                 (conj acc {:vote vote
                            :count (count authors)
                            :authors (mapv :author authors)}))
               [])
       (sort-by :vote)
       (reverse)
       (into [])))

(comment

  (count-votes [{:author "Luke" :vote 3}
                {:author "Yaroslav" :vote 2}
                {:author "Emmanuel" :vote 2}
                {:author "Fra" :vote 1}
                {:author "Stan" :vote 3}
                {:author "Nikhil" :vote 5}]) ;; => [{:vote 5, :count 1, :authors ["Nikhil"]} {:vote 3, :count 2, :authors ["Luke" "Stan"]} {:vote 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:vote 1, :count 1, :authors ["Fra"]}]
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
       :votes [first-vote second-vote]}
      {:result :winner
       :vote first-vote})))

(comment
  (select-winner [{:vote 5, :count 1, :authors ["Nikhil"]} {:vote 3, :count 2, :authors ["Luke" "Stan"]} {:vote 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:vote 1, :count 1, :authors ["Fra"]}]) ;; => {:result :ex-equo, :suggested 3, :votes [2 3]}

  (select-winner [{:vote 5, :count 1, :authors ["Nikhil"]} {:vote 3, :count 2, :authors ["Luke" "Stan"]} {:vote 2, :count 3, :authors ["Yaroslav" "Emmanuel" "Julio"]} {:vote 1, :count 1, :authors ["Fra"]}]) ;; => {:result :winner, :vote 2}
  ,)

(defn can-rediscuss
  [ticket settings]
  (<= (count (:sessions ticket)) (:max-rediscussions settings)))

(comment
 (can-rediscuss ticket settings) ;; true
 (can-rediscuss ticket (update settings :max-rediscussions dec)) ;;false
 ,)

(defn estimate-ticket
  [ticket settings]
  (let [votes (-> ticket :sessions last :votes count-votes)
        delta (votes-delta votes)]

    (if (or (<= delta (:max-vote-delta settings))
            (not (can-rediscuss ticket settings)))
      (select-winner votes)

      (let [{lowest-vote :vote lowest-voters :authors} (last votes)
            {highest-vote :vote highest-voters :authors} (first votes)]
        {:result :discuss
         :highest-vote highest-vote
         :highest-voters highest-voters
         :lowest-vote lowest-vote
         :lowest-voters lowest-voters}))))

(comment

  (estimate-ticket ticket settings) ;; {:result :winner, :vote 2}

  (estimate-ticket {:identifier "PE-12345"
                    :story-points nil
                    :sessions [{:story-points nil
                                :votes [{:author "Yaroslav"
                                         :vote 3}
                                        {:author "Luke"
                                         :vote 2}
                                        {:author "Francesco"
                                         :vote 2}
                                        {:author "Julio"
                                         :vote 3}]}]}
                   settings) ;; => {:result :ex-equo, :suggested 3, :votes [2 3]}

  (estimate-ticket {:identifier "PE-12345"
                    :story-points nil
                    :sessions [{:story-points nil
                                :votes [{:author "Stan"
                                         :vote 0}
                                        {:author "Luke"
                                         :vote 2}
                                        {:author "Francesco"
                                         :vote 4}
                                        {:author "Julio"
                                         :vote 3}]}]}
                   settings) ;; => {:result :discuss, :highest-vote 4, :highest-voters ["Francesco"], :lowest-vote 0, :lowest-voters ["Stan"]}

  (estimate-ticket {:identifier "PE-12345"
                    :story-points nil
                    :sessions [{:story-points nil
                                :votes [{:author "Stan"
                                         :vote 0}
                                        {:author "Luke"
                                         :vote 2}
                                        {:author "Francesco"
                                         :vote 5}
                                        {:author "Julio"
                                         :vote 3}]}
                               {:story-points nil
                                :votes [{:author "Stan"
                                         :vote 0}
                                        {:author "Luke"
                                         :vote 2}
                                        {:author "Francesco"
                                         :vote 4}
                                        {:author "Julio"
                                         :vote 3}]}]}
                   settings) ;;{:result :ex-equo, :suggested 2, :votes [0 2]}
  )

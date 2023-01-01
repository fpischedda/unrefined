(ns fpsd.estimator)

(defn count-votes
  "Return a vector of maps of the result of grouping story points
   and the vote authors, given a map of `votes`.
   Each item of the resulting grouping looks like the following map:
   {:points <some-number>
    :count <how many times voters selected this story points>
    :authors <a vector of strings of the author names>}

   `votes` is a vector of maps looking like the following:
   {:points <some-int> :name <author1-name-string>}
    ...}"
  [votes]
  (->> (group-by :points votes)
       (reduce (fn [acc [points point-and-authors]]
                 (conj acc {:points points
                            :count (count point-and-authors)
                            :authors (mapv (comp :name second) point-and-authors)}))
               [])
       (sort-by :points)
       (reverse)
       (into [])))

(defn votes-delta
  "Return the difference between the maximum and minumum value in the
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

(defn max-rediscussions-reached
  [sessions settings]
  (>= (count sessions) (or (:max-rediscussions settings) 0)))

(defn estimate
  [{:keys [current-session sessions] :as _ticket} settings]
  (if (> (or (:minimum-votes settings) 0)
         (-> current-session :votes count))
      {:result :not-enough-votes}
      (let [votes (-> current-session :votes count-votes)
            delta (votes-delta votes)
            result
            (if (or (< delta (or (:max-points-delta settings) 0))
                    (max-rediscussions-reached sessions settings))
              (select-winner votes)

              (let [{lowest-vote :points lowest-voters :authors} (last votes)
                    {highest-vote :points highest-voters :authors} (first votes)]
                {:result :discuss
                 :highest-vote highest-vote
                 :highest-voters highest-voters
                 :lowest-vote lowest-vote
                 :lowest-voters lowest-voters}))]
        (-> result
            (assoc :votes votes)))))

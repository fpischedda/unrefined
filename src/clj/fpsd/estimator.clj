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

(comment
  (group-by (comp :points second) {"id1" {:points 3 :name "Luke"}
                "id2" {:points 2 :name "Yaroslav"}
                "id3" {:points 2 :name "Emmanuel"}
                "id4" {:points 1 :name "Fra"}
                "id5" {:points 3 :name "Stanislav"}
                "id6" {:points 5 :name "Nikhil"}})

  (count-votes {"id1" {:points 3 :name "Luke"}
                "id2" {:points 2 :name "Yaroslav"}
                "id3" {:points 2 :name "Emmanuel"}
                "id4" {:points 1 :name "Fra"}
                "id5" {:points 3 :name "Stanislav"}
                "id6" {:points 5 :name "Nikhil"}}) ;; => [{:points 5, :count 1, :authors ["Nikhil"]} {:points 3, :count 2, :authors ["Luke" "Stanislav"]} {:points 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:points 1, :count 1, :authors ["Fra"]}]

  (count-votes {"8645c7b2-2049-4346-b75e-196d2ad98ae1" {:points 3, :name "bob", :breakdown {:implementation "", :refactoring "", :tests "", :risk "", :pain ""}}, "b7efd909-a2b4-4d1f-83d1-75fd0ff3190b" {:points 3, :name "alice", :breakdown {:implementation "", :refactoring "", :tests "", :risk "", :pain ""}}}) ;;  => [{:points 3, :count 2, :authors ["bob" "alice"]}]
  )

(defn votes-delta
  "Returns the difference between the maximum and minumum value in the
  provided sorted seq of vote frequencies; an entry looks like:
  [vote [voters...]]"
  [frequencies]
  (- (-> frequencies first (:points 0)) (-> frequencies last (:points 0))))

(comment
  (votes-delta [{:points 5, :count 1, :authors ["Nikhil"]} {:points 3, :count 2, :authors ["Luke" "Stan"]} {:points 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:points 1, :count 1, :authors ["Fra"]}] ) ;; => 4
  ,)

(defn select-winner
  [counted-votes]
  (let [sorted (reverse (sort-by :count counted-votes))
        [{first-vote :points first-voters :authors}
         {second-vote :points second-voters :authors} _] sorted]
    (if (= (count first-voters) (count second-voters))
      {:result :ex-equo
       :suggested (max first-vote second-vote)
       :same-points [[first-vote first-voters] [second-vote second-voters]]}
      {:result :winner
       :points first-vote})))

(comment
  (select-winner [{:points 5, :count 1, :authors ["Nikhil"]} {:points 3, :count 2, :authors ["Luke" "Stan"]} {:points 2, :count 2, :authors ["Yaroslav" "Emmanuel"]} {:points 1, :count 1, :authors ["Fra"]}]) ;; => {:result :ex-equo, :suggested 3, :same-votes [2 3]}

  (select-winner [{:points 5, :count 1, :authors ["Nikhil"]} {:points 3, :count 2, :authors ["Luke" "Stan"]} {:points 2, :count 3, :authors ["Yaroslav" "Emmanuel" "Julio"]} {:points 1, :count 1, :authors ["Fra"]}]) ;; => {:result :winner, :points 2}
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
        (if (or (< delta (:max-points-delta settings))
                (not (can-rediscuss sessions settings)))
          (select-winner votes)

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

  (count-votes (-> fpsd.refinements/refinements_ deref (get "HUHGJF") :tickets (get "asdf") :current-session :votes))

  (votes-delta (count-votes (-> fpsd.refinements/refinements_ deref (get "HUHGJF") :tickets (get "asdf") :current-session :votes)))

  (-> fpsd.refinements/refinements_ deref (get "HUHGJF") :tickets (get "asdf") :current-session) ;; {:status :open, :result nil, :votes {"eb550ebf-e206-471f-850c-e9e80443a235" {:points 3, :name "bob", :breakdown {:implementation "", :refactoring "", :tests "", :risk "", :pain ""}}, "4196502c-edb5-4c23-8861-4f042d8ecfed" {:points 3, :name "alice", :breakdown {:implementation "", :refactoring "", :tests "", :risk "", :pain ""}}}, :skips #{}}
  (estimate (-> fpsd.refinements/refinements_ deref (get "HUHGJF") :tickets (get "asdf")) settings) ;; {:result :winner, :points 3, :votes [{:points 3, :count 2, :authors ["bob" "alice"]}]}

  (estimate ticket settings) ;; {:result :winner, :points 2}

  (estimate {:current-session {:story-points nil
                               :points {"1" {:points 3 :name "Bob"}
                                        "2" {:points 2 :name "Alice"}
                                        "3" {:points 2 :name "Joe"}
                                        "4" {:points 3 :name "Foo"}}}
             :sessions []}
            settings) ;; => {:result :ex-equo, :suggested 3, :same-points [[2 ["Alice" "Joe"]] [3 ["Bob" "Foo"]]], :votes [{:points 3, :count 2, :authors ["Bob" "Foo"]} {:points 2, :count 2, :authors ["Alice" "Joe"]}]}

  (estimate {:current-session {:story-points nil
                               :points {"1" {:points 0 :name "Bob"}
                                        "2" {:points 1 :name "Alice"}
                                        "3" {:points 4 :name "Joe"}
                                        "4" {:points 3 :name "Foo"}}}
             :sessions []}
            settings) ;; {:result :discuss, :highest-vote 4, :highest-voters ["Joe"], :lowest-vote 0, :lowest-voters ["Bob"], :votes [{:points 4, :count 1, :authors ["Joe"]} {:points 3, :count 1, :authors ["Foo"]} {:points 1, :count 1, :authors ["Alice"]} {:points 0, :count 1, :authors ["Bob"]}]}

  (estimate {:current-session {:story-points nil
                               :points {"1" {:points 3 :name "Bob"}
                                        "2" {:points 2 :name "Alice"}
                                        "3" {:points 1 :name "Joe"}
                                        "4" {:points 3 :name "Foo"}}}
             :sessions [{:story-points nil
                         :points {"1" {:points 4 :name "Bob"}
                                  "2" {:points 2 :name "Alice"}
                                  "3" {:points 2 :name "Joe"}
                                  "4" {:points 0 :name "Foo"}}}]}
            settings) ;; => {:result :winner, :points 3, :votes [{:points 3, :count 2, :authors ["Bob" "Foo"]} {:points 2, :count 1, :authors ["Alice"]} {:points 1, :count 1, :authors ["Joe"]}]}
  )

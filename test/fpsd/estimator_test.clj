(ns fpsd.estimator-test
    (:require
     [clojure.test :refer [are deftest is testing]]
     [fpsd.estimator :as estimator]
     [fpsd.refinements :as refinements]))

(testing "Estimator utils"
  (deftest count-votes
    (is (= (estimator/count-votes {"id1" {:points 3 :name "Bob"}
                                   "id2" {:points 2 :name "Alice"}
                                   "id3" {:points 2 :name "Mario"}
                                   "id4" {:points 1 :name "Frank"}
                                   "id5" {:points 3 :name "Foo"}
                                   "id6" {:points 5 :name "Bar"}})

           [{:points 5, :count 1, :authors ["Bar"]}
            {:points 3, :count 2, :authors ["Bob" "Foo"]}
            {:points 2, :count 2, :authors ["Alice" "Mario"]}
            {:points 1, :count 1, :authors ["Frank"]}])))

  (deftest votes-delta
    (is (= 4 (estimator/votes-delta
              [{:points 5, :count 1, :authors ["Bar"]}
               {:points 3, :count 2, :authors ["Bob" "Foo"]}
               {:points 2, :count 2, :authors ["Alice" "Mario"]}
               {:points 1, :count 1, :authors ["Frank"]}]))))

  (deftest max-rediscussions
    (are [sessions expected]
         (= (estimator/max-rediscussions-reached
             sessions
             {:max-rediscussions 1})
            expected)

      [] false

      [{:votes {"1" {:points 1 :name "Bob"}
                "2" {:points 4 :name "Alice"}}}] true)))

(testing "Select winner"
  (deftest select-winner
    (are [votes expected]
         (= (estimator/select-winner votes)
            expected)

      ;; clear winner case
      [{:points 3, :count 3, :authors ["Bob" "Foo" "Bar"]}
       {:points 2, :count 2, :authors ["Alice" "Mario"]}
       {:points 1, :count 1, :authors ["Frank"]}]

      {:result :winner :points 3}

      ;; ex-equo case
      [{:points 3, :count 2, :authors ["Bob" "Foo"]}
       {:points 2, :count 2, :authors ["Alice" "Mario"]}
       {:points 1, :count 1, :authors ["Frank"]}]

      {:result :ex-equo
       :suggested 3
       :same-points [{:authors ["Alice" "Mario"] :points 2}
                     {:authors ["Bob" "Foo"] :points 3}]})))

(testing "Estimating a ticket"
  (deftest estimate
    (are [ticket expected]
         (= (estimator/estimate ticket refinements/default-settings)
            expected)

      ;; clear winner
      {:current-session
       {:votes
        {"id1" {:points 3 :name "Bob"}
         "id2" {:points 2 :name "Alice"}
         "id3" {:points 2 :name "Mario"}
         "id4" {:points 1 :name "Frank"}
         "id5" {:points 3 :name "Foo"}
         "id6" {:points 3 :name "Bar"}}}
       :sessions []}

      {:result :winner
       :points 3
       :votes [{:authors ["Bob" "Foo" "Bar"], :count 3, :points 3}
               {:authors ["Alice" "Mario"], :count 2, :points 2}
               {:authors ["Frank"], :count 1, :points 1}]}

      ;; ex-equo
      {:current-session {:votes {"1" {:points 3 :name "Bob"}
                                 "2" {:points 2 :name "Alice"}
                                 "3" {:points 2 :name "Joe"}
                                 "4" {:points 3 :name "Foo"}}}
       :sessions []}

      {:result :ex-equo
       :suggested 3
       :same-points [{:authors ["Alice" "Joe"], :points 2}
                     {:authors ["Bob" "Foo"], :points 3}]
       :votes [{:points 3 :count 2 :authors ["Bob" "Foo"]}
               {:points 2 :count 2 :authors ["Alice" "Joe"]}]}

      ;; discuss
      {:current-session {:votes {"1" {:points 1 :name "Bob"}
                                 "2" {:points 2 :name "Alice"}
                                 "3" {:points 4 :name "Joe"}
                                 "4" {:points 3 :name "Foo"}}}
       :sessions []}

      {:result :discuss
       :highest-vote 4
       :highest-voters ["Joe"]
       :lowest-vote 1
       :lowest-voters ["Bob"]
       :votes [{:points 4 :count 1 :authors ["Joe"]}
               {:points 3 :count 1 :authors ["Foo"]}
               {:points 2 :count 1 :authors ["Alice"]}
               {:points 1 :count 1 :authors ["Bob"]}]}

      ;; select most voted after discussion case
      {:current-session {:votes {"1" {:points 1 :name "Bob"}
                                 "2" {:points 3 :name "Alice"}
                                 "3" {:points 4 :name "Joe"}
                                 "4" {:points 3 :name "Foo"}}}
       :sessions [{:votes {"1" {:points 1 :name "Bob"}
                           "2" {:points 2 :name "Alice"}
                           "3" {:points 4 :name "Joe"}
                           "4" {:points 3 :name "Foo"}}}]}

      {:result :winner
       :points 3
       :votes [{:authors ["Joe"] :points 4 :count 1}
               {:authors ["Alice" "Foo"] :points 3 :count 2}
               {:authors ["Bob"] :points 1 :count 1}]}

      ;; not enough voters
      {:current-session {:votes {"1" {:points 2 :name "Foo"}}}}
      {:result :not-enough-votes})))

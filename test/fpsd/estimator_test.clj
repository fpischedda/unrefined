(ns fpsd.estimator-test
    (:require
     [clojure.test :refer [deftest is testing]]
     [fpsd.estimator :as estimator]))

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
               {:points 1, :count 1, :authors ["Frank"]}] ))))

  (deftest max-rediscussions-not-reached
    (is (not (estimator/max-rediscussions-reached
              []
              {:max-rediscussions 1}))))

  (deftest max-rediscussions-reached
    (is (estimator/max-rediscussions-reached
              [{:votes {"1" {:points 1 :name "Bob"}
                        "2" {:points 4 :name "Alice"}}}]
              {:max-rediscussions 1}))))

(testing "Select winner"
  (deftest winner-case
    (is (= {:result :winner :points 3}
           (estimator/select-winner
            [{:points 3, :count 3, :authors ["Bob" "Foo" "Bar"]}
             {:points 2, :count 2, :authors ["Alice" "Mario"]}
             {:points 1, :count 1, :authors ["Frank"]}]))))
  (deftest ex-æquo-case
    (is (= {:result :ex-equo
            :suggested 3
            :same-points [{:authors ["Alice" "Mario"] :points 2}
                          {:authors ["Bob" "Foo"] :points 3}]}
           (estimator/select-winner
            [{:points 3, :count 2, :authors ["Bob" "Foo"]}
             {:points 2, :count 2, :authors ["Alice" "Mario"]}
             {:points 1, :count 1, :authors ["Frank"]}])))))

(testing "Estimating a ticket"
  (deftest winner-case
    (is (= {:result :winner
            :points 3
            :votes [{:authors ["Bob" "Foo" "Bar"], :count 3, :points 3}
                    {:authors ["Alice" "Mario"], :count 2, :points 2}
                    {:authors ["Frank"], :count 1, :points 1}]}
           (estimator/estimate
            {:current-session
             {:votes
              {"id1" {:points 3 :name "Bob"}
               "id2" {:points 2 :name "Alice"}
               "id3" {:points 2 :name "Mario"}
               "id4" {:points 1 :name "Frank"}
               "id5" {:points 3 :name "Foo"}
               "id6" {:points 3 :name "Bar"}}}
             :sessions []}
            {:max-rediscussions 1
             :max-points-delta 3}))))

  (deftest ex-æquo-case
    (is (= {:result :ex-equo
            :suggested 3
            :same-points [{:authors ["Alice" "Joe"], :points 2}
                          {:authors ["Bob" "Foo"], :points 3}]
            :votes [{:points 3 :count 2 :authors ["Bob" "Foo"]}
                    {:points 2 :count 2 :authors ["Alice" "Joe"]}]}
           (estimator/estimate
            {:current-session {:votes {"1" {:points 3 :name "Bob"}
                                       "2" {:points 2 :name "Alice"}
                                       "3" {:points 2 :name "Joe"}
                                       "4" {:points 3 :name "Foo"}}}
             :sessions []}
            {:max-rediscussions 1
             :max-points-delta 3}))))

  (deftest discuss-case
    (is (= {:result :discuss
            :highest-vote 4
            :highest-voters ["Joe"]
            :lowest-vote 1
            :lowest-voters ["Bob"]
            :votes [{:points 4 :count 1 :authors ["Joe"]}
                    {:points 3 :count 1 :authors ["Foo"]}
                    {:points 2 :count 1 :authors ["Alice"]}
                    {:points 1 :count 1 :authors ["Bob"]}]}
           (estimator/estimate
            {:current-session {:votes {"1" {:points 1 :name "Bob"}
                                       "2" {:points 2 :name "Alice"}
                                       "3" {:points 4 :name "Joe"}
                                       "4" {:points 3 :name "Foo"}}}
             :sessions []}
            {:max-rediscussions 1
             :max-points-delta 3}))))

  (deftest select-most-voted-after-discussion-case
    (is (= {:result :winner
            :points 3
            :votes [{:authors ["Joe"] :points 4 :count 1}
                    {:authors ["Alice" "Foo"] :points 3 :count 2}
                    {:authors ["Bob"] :points 1 :count 1}]}
           (estimator/estimate
            {:current-session {:votes {"1" {:points 1 :name "Bob"}
                                       "2" {:points 3 :name "Alice"}
                                       "3" {:points 4 :name "Joe"}
                                       "4" {:points 3 :name "Foo"}}}
             :sessions [{:votes {"1" {:points 1 :name "Bob"}
                                 "2" {:points 2 :name "Alice"}
                                 "3" {:points 4 :name "Joe"}
                                 "4" {:points 3 :name "Foo"}}}]}
            {:max-rediscussions 1
             :max-points-delta 3})))))

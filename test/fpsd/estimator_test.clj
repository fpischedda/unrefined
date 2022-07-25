(ns fpsd.estimator-test
    (:require
     [clojure.test :refer [deftest is testing]]
     [fpsd.estimator :refer [multiply]]))

(testing "Test"
  (is (= 1 1)))
#_(testing "Estimator"
  (testing "Count votes"
    (is (= (count-votes {"id1" {:points 3 :name "Bob"}
                         "id2" {:points 2 :name "Alice"}
                         "id3" {:points 2 :name "Mario"}
                         "id4" {:points 1 :name "Frank"}
                         "id5" {:points 3 :name "Foo"}
                         "id6" {:points 5 :name "Bar"}})

           [{:points 5, :count 1, :authors ["Bar"]}
            {:points 3, :count 2, :authors ["Bob" "Foo"]}
            {:points 2, :count 2, :authors ["Alice" "Mario"]}
            {:points 1, :count 1, :authors ["Frank"]}]))))

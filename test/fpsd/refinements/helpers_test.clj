(ns fpsd.refinements.helpers-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [fpsd.refinements.helpers :as helpers]))

(testing "Extract ticket id from URL"
  (deftest extract-jira-id
    (is (= "PE-1234" (helpers/extract-ticket-id-from-url "https://cargo-one.atlassian.net/browse/PE-1234"))))

  (deftest extract-github-id
    (is (= "12" (helpers/extract-ticket-id-from-url "https://github.com/fpischedda/unrefined/issues/12"))))

  (deftest extract-id-fails
    (is (nil? (helpers/extract-ticket-id-from-url "https://some-garbage")))))

(testing "try-parse-int tries to convert a string to int, if it falis return nil"
  (deftest try-parse-int-success
    (is (= 5 (helpers/try-parse-int "5"))))

  (deftest try-parse-int-fail
    (is (nil? (helpers/try-parse-int "five")))))

(testing "get-vote-from-params extract vote, name and breakdown from request body"
  (deftest all-defaults-when-empty-params
    (is (= {:points nil
            :name "Anonymous Coward"
            :breakdown {}}
           (helpers/get-vote-from-params
            {}))))

  (deftest get-partial-supported-breakdowns
    (is (= {:points nil
            :name "Bob"
            :breakdown {:tests "1"
                         :pain "2"}}
           (helpers/get-vote-from-params
            {:name "Bob"
             :tests "1"
             :pain "2"}))))

  (deftest parse-points-correctly
    (is (= {:points 3
            :name "Bob"
            :breakdown {:tests "1"
                         :pain "2"}}
           (helpers/get-vote-from-params
            {:name "Bob"
             :points "3"
             :tests "1"
             :pain "2"})))))

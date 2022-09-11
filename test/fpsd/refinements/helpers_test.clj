(ns fpsd.refinements.helpers-test
  (:require
   [clojure.test :refer [are deftest is testing]]
   [fpsd.refinements.helpers :as helpers]))

(testing "Extract ticket id from URL"
  (deftest extract-jira-id
    (are [url id]
         (= (helpers/extract-ticket-id-from-url url) id)

      ;; jira
      "https://cargo-one.atlassian.net/browse/PE-1234" "PE-1234"

      ;; github
      "https://github.com/fpischedda/unrefined/issues/12" "12"

      ;; trello
      "https://trello.com/c/xAaL7xNa/3-parse-trellos-card-url-to-extract-description-and-id"
      "parse trellos card url to extract description and id"

      ;; failed
      "https://some-garbage" nil)))

(testing "try-parse-int tries to convert a string to int, if it falis return nil"
  (deftest try-parse-int-success
    (is (= 5 (helpers/try-parse-int "5"))))

  (deftest try-parse-int-fail
    (is (nil? (helpers/try-parse-int "five")))))

(testing "get-vote-from-params extract vote, name and breakdown from request body"
  (deftest all-defaults-when-empty-params
    (are [params expected]
         (= (helpers/get-vote-from-params params) expected)

      ;; all defaults on empty params
      {}
      {:points nil
       :name "Anonymous Coward"
       :breakdown {}}

      ;; only parse breakdown
      {:name "Bob"
       :tests "1"
       :pain "2"}
      {:points nil
       :name "Bob"
       :breakdown {:tests "1"
                   :pain "2"}}

      ;; parse all
      {:name "Bob"
       :points "3"
       :tests "1"
       :pain "2"}
      {:points 3
       :name "Bob"
       :breakdown {:tests "1"
                   :pain "2"}})))

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

(testing "try-parse-int tries to convert a string to int, if it falis return nil or default"
  (deftest try-parse-int-success
    (is (= 5 (helpers/try-parse-int "5"))))

  (deftest try-parse-int-fail
    (is (nil? (helpers/try-parse-int "five"))))
  
  (deftest try-parse-int-uses-default
    (is (= 0 (helpers/try-parse-int "five" 0)))))

(testing "try-parse-long tries to convert a string to long, if it falis return nil or default"
  (deftest try-parse-long-success
    (is (= 5 (helpers/try-parse-long "5"))))

  (deftest try-parse-long-fail
    (is (nil? (helpers/try-parse-long "five"))))
  
  (deftest try-parse-long-uses-default
    (is (= 0 (helpers/try-parse-long "five" 0)))))

(testing "get-breakdown-from-params"
  (deftest skip-missing-breakdown
    (is (= {} (helpers/get-breakdown-from-params {:new-one "1"} [:missing]))))

  (deftest skip-empty-breakdown
    (is (= {} (helpers/get-breakdown-from-params {:item ""} [:item]))))

  (deftest only-selects-available-breakdown
    (is (= {:available 1} (helpers/get-breakdown-from-params {:available "1"
                                                              :not-available "2"} [:available])))))

(testing "get-estimation-from-params extract vote, name and breakdown from request body"
  (deftest all-defaults-when-empty-params
    (are [params expected]
         (= (helpers/get-estimation-from-params params "general")
            expected)

      ;; all defaults on empty params
      {}
      {:score 0
       :author-name "Anonymous Coward"
       :breakdown {}
       :skipped? false}

      ;; only parse breakdown
      {:name "Bob"
       :testing "1"
       :backend "2"}
      {:score 0
       :author-name "Bob"
       :breakdown {:testing 1
                   :backend 2}
       :skipped? false}

      ;; parse all
      {:name "Bob"
       :points "3"
       :testing "1"
       :backend "2"}
      {:score 3
       :author-name "Bob"
       :breakdown {:testing 1
                   :backend 2}
       :skipped? false}

      ;; skipping
      {:name "Skipper"
       :skip-button 1}
      {:author-name "Skipper"
       :score 0
       :breakdown {}
       :skipped? true}
      )))

(testing "Loading cheatsheets"
  (deftest assure-all-expected-cheatsheets-are-loaded
    (is (= '("default" "generic")
           (keys (helpers/get-all-cheatsheets))))))

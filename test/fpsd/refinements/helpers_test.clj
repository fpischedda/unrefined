(ns fpsd.refinements.helpers-test
  (:require
   [clojure.test :refer [are deftest is testing]]
   [fpsd.refinements.helpers :as helpers]))

(deftest extract-jira-id
  (testing "Extract ticket id from URL"
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

(deftest try-parse-int
  (testing "try-parse-int success"
    (is (= 5 (helpers/try-parse-int "5"))))

  (testing "try-parse-int fails returning nil, no default provided"
    (is (nil? (helpers/try-parse-int "five"))))
  
  (testing "try-parse-int fails but returns a default value"
    (is (= 0 (helpers/try-parse-int "five" 0)))))

(deftest try-parse-long
  (testing "try-parse-long success"
    (is (= 5 (helpers/try-parse-long "5"))))

  (testing "try-parse-long fails returning nil, no default provided"
    (is (nil? (helpers/try-parse-long "five"))))
  
  (testing "try-parse-long  fails but returns a default value"
    (is (= 0 (helpers/try-parse-long "five" 0)))))

(deftest get-breakdown-from-params
  (testing "skip missing breakdown items"
    (is (= {} (helpers/get-breakdown-from-params {:new-one "1"} [:missing]))))

  (testing "skip empty breakdown, user did not provide a value for it"
    (is (= {} (helpers/get-breakdown-from-params {:item ""} [:item]))))

  (testing "given available and unknown breakdowns keep only available ones"
    (is (= {:available 1} (helpers/get-breakdown-from-params {:available "1"
                                                              :not-available "2"} [:available])))))

(deftest all-defaults-when-empty-params
  (testing "get-estimation-from-params extract vote, name and breakdown from request body"
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

(deftest get-all-cheatsheets
  (testing "assure all expected cheatsheets are loaded"
    (is (= '("default" "generic")
           (keys (helpers/get-all-cheatsheets))))))

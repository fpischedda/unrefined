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

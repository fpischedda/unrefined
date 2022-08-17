(ns fpsd.refinements.helpers
  (:require [clojure.string :as str]))

(def url-parsers [[#"https://.*/issues/(.*)" second]
                  [#"https://.*/browse/(.*)" second]
                  [#"https://trello.com/c/\w+/\d+-(.*)" (fn [match] (-> match second (str/replace #"-" " ")))]])

(defn extract-ticket-id-from-url
  "Return the ticket id extracted from the URL of the ticketing system,
   if the format is not recognized then return nil"
  [url]
  (when url
    (some (fn [[regex transform-fn]] (some->> url (re-matches regex) transform-fn))
          url-parsers)))

(defn try-parse-int
  "Return the integer value represented by the string int-str
   or nil if it is not possible to parse the number"
  [int-str]
  (try (Integer/parseInt int-str)
       (catch NumberFormatException _ nil)))

(def supported-breakdowns [:implementation :tests :migrations :refactoring :risk :pain])

(defn get-vote-from-params
  [params]
  {:points (-> params :points try-parse-int)
   :name (or (-> params :name) "Anonymous Coward")
   :breakdown
   (select-keys params supported-breakdowns)})

(defn utc-now
  []
  (java.time.LocalDateTime/now))

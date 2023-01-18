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
   or default if it is not possible to parse the number"
  ([str-value]
   (try-parse-int str-value nil))
  ([str-value default]
   (try (Integer/parseInt str-value)
        (catch NumberFormatException _ default))))

(defn try-parse-long
  "Return the integer value represented by the string int-str
   or default if it is not possible to parse the number"
  ([str-value]
   (try-parse-long str-value nil))
  ([str-value default]
   (try (Long/parseLong str-value)
        (catch NumberFormatException _ default))))

(def initial-supported-breakdowns_
  [:implementation :backend :migrations :data_migrations :testing :manual_testing :risk :complexity])

(defn get-breakdown-from-params
  [params supported-breakdowns]
  (reduce (fn [acc breakdown]
             (if-let [value (get params breakdown)]
               (assoc acc breakdown (try-parse-long value 0))
               acc))
           {} supported-breakdowns))

(defn get-vote-from-params
  [params]
  {:points (-> params :points (try-parse-long 0))
   :name (or (params :name) "Anonymous Coward")
   :skipped? (some? (:skip-button params))
   :breakdown (get-breakdown-from-params params initial-supported-breakdowns_)})

(defn utc-now
  []
  (java.util.Date.))

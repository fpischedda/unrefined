(ns one.unrefined.refinements.helpers
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

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

(def cheatsheets-root "public/estimation-cheatsheets")

(defn load-cheatsheet [name]
  (-> (str cheatsheets-root "/" name ".json")
      io/resource
      io/reader
      (json/parse-stream true)))

(defn get-all-cheatsheets []
  (reduce (fn [acc name]
            (assoc acc name (load-cheatsheet name)))
          {} ["default" "generic"]))

(def cheatsheet-map_
  (reduce-kv (fn [acc name body]
               (assoc acc name (:estimationTopics body)))
             {} (get-all-cheatsheets)))

(def breakdown-cheatsheet-map_
  (reduce-kv (fn [acc name breakdowns]
               (assoc acc name (mapv (fn [item] (keyword (:name item)))
                                     breakdowns)))
             {} cheatsheet-map_))

(defn breakdowns-for-cheatsheet
  [cheatsheet]
  (if-let [breakdowns (get breakdown-cheatsheet-map_ cheatsheet)]
    breakdowns
    (get breakdown-cheatsheet-map_ "default")))

(defn get-breakdown-from-params
  [params supported-breakdowns]
  (reduce (fn [acc breakdown]
             (if-let [value (try-parse-long (get params breakdown))]
               (assoc acc breakdown value)
               acc))
           {} supported-breakdowns))

(defn get-estimation-from-params
  [params cheatsheet]
  {:score (-> params :points (try-parse-long 0))
   :author-name (or (params :name) "Anonymous Coward")
   :skipped? (some? (:skip-button params))
   :breakdown (get-breakdown-from-params params (breakdowns-for-cheatsheet cheatsheet))})

(defn utc-now
  []
  (java.util.Date.))

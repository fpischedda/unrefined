(ns fpsd.refinements.helpers)

(def url-regexes [#"https://.*/issues/(.*)" #"https://.*/browse/(.*)"])

(defn extract-ticket-id-from-url
  "Return the ticket id extracted from the URL of the ticketing system,
   if the format is not recognized then return nil"
  [url]
  (reduce (fn [_ url-regex]
            (some->> url (re-matches url-regex) second reduced))
          nil url-regexes))

(defn try-parse-int
  "Return the integer value represented by the string int-str
   or nil if it is not possible to parse the number"
  [int-str]
  (try (Integer/parseInt int-str)
       (catch NumberFormatException _ nil)))

(defn get-vote-from-params
  [params]
  {:points (-> params :points try-parse-int)
   :name (or (-> params :name) "Anonymous Coward")
   :breakdown
   (reduce (fn [acc item]
             (if-let [value (get params item)]
               (assoc acc item value)
               acc))
           {}
           [:implementation :tests :migrations :refactoring :risk :pain])})

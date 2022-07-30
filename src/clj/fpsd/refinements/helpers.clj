(ns fpsd.refinements.helpers)

(def url-regexes [#"https://.*/issues/(.*)" #"https://.*/browse/(.*)"])

(defn extract-ticket-id-from-url
  "Return the ticket id extracted from the URL of the ticketing system,
   if the format is not recognized then return nil"
  [url]
  (reduce (fn [_ url-regex]
            (some->> url (re-matches url-regex) second reduced))
          nil url-regexes))

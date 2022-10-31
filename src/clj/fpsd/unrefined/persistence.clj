(ns fpsd.unrefined.persistence
  (:require [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder encode-str]]
            [fpsd.configuration :refer [config]]))

(add-encoder java.time.LocalDateTime encode-str)

(defn get-ticket-path
  "Return the full path to the ticket data store"
  [code ticket-id]
  (format "%s/%s-%s.json" (-> config :persistence :path) code ticket-id))

(defn store-ticket-to-file!
  "Store the ticket and refinement data to file"
  [refinement ticket estimation]
  (let [filename (get-ticket-path (:code refinement) (:id ticket))
        contents (json/generate-string {:refinement (dissoc refinement :tickets)
                                        :ticket ticket
                                        :estimation estimation})]
    (spit filename contents)))

(defn get-stored-ticket
  "Read the content of the ticket from file and returns it"
  [code ticket-id]
  (-> (get-ticket-path code ticket-id)
      slurp
      (json/parse-string true)
      (update-in [:estimation :result] keyword)))

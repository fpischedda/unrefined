(ns fpsd.unrefined.persistence.json-file
  (:require [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder encode-str]]
            [fpsd.unrefined.persistence :refer [store-ticket! get-stored-ticket]]))

(add-encoder java.time.LocalDateTime encode-str)

(defn get-ticket-path
  "Return the full path to the ticket data store"
  [configuration code ticket-id]
  (format "%s/%s-%s.json" (:path configuration) code ticket-id))

(defmethod store-ticket! :json-file
  [configuration code ticket-id data]
  (let [filename (get-ticket-path configuration code ticket-id)
        contents (json/generate-string data)]
    (spit filename contents)))

(defmethod get-stored-ticket :json-file
  [configuration code ticket-id]
  (-> (get-ticket-path configuration code ticket-id)
      slurp
      (json/parse-string true)
      (update-in [:estimation :result] keyword)))

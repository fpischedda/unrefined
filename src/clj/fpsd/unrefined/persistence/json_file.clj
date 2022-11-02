(ns fpsd.unrefined.persistence
  (:require [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder encode-str]]
            [fpsd.unrefined.persistence :refer [store-ticket-backend! get-ticket-backend]]))

(add-encoder java.time.LocalDateTime encode-str)

(defn get-ticket-path
  "Return the full path to the ticket data store"
  [configuration code ticket-id]
  (format "%s/%s-%s.json" (:path configuration) code ticket-id))

(defmethod store-ticket-backend! :json-file
  [configuration code ticket-id data]
  (let [filename (get-ticket-path configuration code ticket-id)
        contents (json/generate-string data)]
    (spit filename contents)))

(defmethod get-ticket-backend :json-file
  [configuration code ticket-id]
  (-> (get-ticket-path configuration code ticket-id)
      slurp
      (json/parse-string true)
      (update-in [:estimation :result] keyword)))

(ns fpsd.unrefined.persistence
  (:require [fpsd.configuration :refer [config]]))

(defmulti store-ticket-backend!
  (fn [configuration _code _ticket-id _data] (:backend configuration)))

(defmulti get-ticket-backend
  (fn [configuration _code _ticket-id] (:backend configuration)))

(defn store-ticket!
  "Store the ticket and refinement data according to the selected backend"
  [code ticket-id data]
  (store-ticket-backend! (:persistence config) code ticket-id data))

(defn get-stored-ticket
  "Read the content of the ticket from file and returns it"
  [code ticket-id]
  (get-ticket-backend (:persistence config) code ticket-id))

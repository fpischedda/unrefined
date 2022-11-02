(ns fpsd.unrefined.persistence
  (:require [fpsd.configuration :refer [config]]))

(defmulti store-ticket!
  (fn [configuration _code _ticket-id _data] (:backend configuration)))

(defmulti get-stored-ticket
  (fn [configuration _code _ticket-id] (:backend configuration)))

(ns fpsd.configuration
  (:require [mount.core :as mount]
            [environ.core :refer [env]]))

(defn safe-to-int
  [maybe-int]
  (if (string? maybe-int)
    (Integer/parseInt maybe-int)
    maybe-int))

(mount/defstate config
  :start {:http  {:port (safe-to-int (:unrefined-http-port env 8080))}
          :nrepl {:port (safe-to-int (:unrefined-nrepl-port env 1667))}
          :logging {:type :simple-file
                    :filename (:unrefined-log-file env "/tmp/unrefined.log")}
          ;; use the filesystem as storage medium
          :datahike {:store {:backend :file
                             :path (:unrefined-datahike-db env "/tmp/unrefined.datahike")}
                     :name "unrefined"
                     :schema-flexibility :write
                     :keep-history? true}})

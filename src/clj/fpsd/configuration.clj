(ns fpsd.configuration
  (:require [mount.core :as mount]
            [environ.core :refer [env]]))

(def default-ttl (* 60 60 12))

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
                             :path "/tmp/unrefined.datahike"}
                     :name "unrefined"
                     :schema-flexibility :write
                     :keep-history? true}
          :persistence {:backend :json-file
                        :path (:unrefined-tickets-path env "/tmp/")}
          :mr-clean {:ttl (safe-to-int (:unrefined-refinement-ttl env default-ttl))}})

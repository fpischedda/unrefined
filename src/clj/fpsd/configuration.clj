(ns fpsd.configuration
  (:require [mount.core :as mount]
            [environ.core :refer [env]]))

(mount/defstate config
  :start {:http  {:port (:unrefined-http-port env 8080)}
          :nrepl {:port (:unrefined-nrepl-port env 1667)}
          :logging {:type :simple-file
                    :filename (:unrefined-log-file env "/tmp/unrefined.log")}
          :persistence {:path (:unrefined-tickets-path env "/tmp/")}
          :mr-clean {:ttl (:unrefined-refinement-ttl env (* 60 60 12))}})

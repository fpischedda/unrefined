(ns fpsd.configuration
  (:require [mount.core :as mount]
            [environ.core :refer [env]]))

(mount/defstate config
  :start {:http  {:port (:unrefined-http-port env 8080)}
          :nrepl {:port (:unrefined-nrepl-port env 1667)}
          :mr-clean {:ttl (:unrefined-refinement-ttl env (* 60 60 12))}})

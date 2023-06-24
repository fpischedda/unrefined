(ns one.unrefined.nrepl
  (:require
   [mount.core :as mount]
   [nrepl.server :as nrepl]
   [one.unrefined.configuration :refer [config]]))

(mount/defstate nrepl-server
  :start (nrepl/start-server {:port (-> config :nrepl :port)})
  :stop (nrepl/stop-server nrepl-server))

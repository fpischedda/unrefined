(ns fpsd.unrefined.server
  (:require [mount.core :as mount]
            [fpsd.configuration :refer [config]]
            [fpsd.routes :refer [http-server]]
            [fpsd.unrefined.nrepl :refer [nrepl-server]])
  (:gen-class))

(defn -main [& _args]
  (println "Starting unrefined service...")
  (println (mount/start))
  (println "Datahike config" (-> config :datahike))
  (println "Ready to accept nrepl connections at" (-> config :nrepl :port))
  (println "Ready to accept http connections at" (-> config :http :port))
  (println "Startup config" config)

  (loop []
    (Thread/sleep 10000)
    (recur))
  (println "Stopping unrefined service")
  (mount/stop))

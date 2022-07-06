(ns fpsd.configuration
  (:require [mount.core :as mount]))

(mount/defstate config
  :start {:http  {:port 8080}
          :nrepl {:port 1337}})

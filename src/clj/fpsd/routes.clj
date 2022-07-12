(ns fpsd.routes
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [portal.api :as portal]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [fpsd.configuration :refer [config]]
            [fpsd.refinements.handlers :as handlers]))

(comment
  (def p (portal/open))
  (add-tap #'portal/submit)

  (portal/close p)
  ,)

(defn add-headers [handler fixed-headers]
  (fn [request]
      (let [response (handler request)]
        (update response :headers (fn [headers]
                                    (if headers
                                      (merge headers fixed-headers)
                                      fixed-headers))))))

(defn common-cookies [handler]
  (fn [request]
    (let [user-id (-> request :cookies (get "user-id") :value)
          name (-> request :cookies (get "name") :value)]
      (handler (assoc request :common-cookies {:user-id user-id
                                               :name name})))))

(defn tap-request-response [handler]
  (fn [request]
    (tap> request)
    (let [response (handler request)]
      (tap> response)
      response)))

(def handler
  (ring/ring-handler
   (ring/router
    [["/" {:get handlers/index}]
     ["/assets/*" (ring/create-resource-handler)]

     ["/refine"
      ["" {:get handlers/index
           :post handlers/create-refinement}]

      ["/:code/ticket/:ticket-id" {:get handlers/estimate-watch}]
      ["/:code/ticket/:ticket-id/reveal" {:get handlers/estimate-reveal}]

      ["/:code/ticket/:ticket-id/estimate" {:get handlers/estimate-view
                                            :post handlers/estimate-done}]
      ["/:code/ticket" {:post handlers/add-ticket}]
      ["/:code/events" {:get handlers/events-stream-handler}]]]

    {:data {:muuntaja m/instance
            :middleware [wrap-cookies
                         wrap-session
                         wrap-params
                         wrap-keyword-params
                         common-cookies
                         ;; tap-request-response
                         muuntaja/format-middleware]}})))

(mount/defstate http-server
  :start (http/start-server #'handler {:port (-> config :http :port)})
  :stop (.close http-server))

(comment
  (mount/start)
  (mount/stop)
  )

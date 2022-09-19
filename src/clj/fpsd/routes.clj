(ns fpsd.routes
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.coercion :as rrc]
            [reitit.coercion.malli]
            [reitit.core :as r]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [fpsd.configuration :refer [config]]
            [fpsd.refinements.handlers :as handlers]))

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

(def app-router
  (ring/router
   [["/" {:get handlers/index}]
    ["/assets/*" (ring/create-resource-handler)]

    ["/refine"
     ["" {:get handlers/index
          :post {:handler handlers/create-refinement
                 ;; :coercion reitit.coercion.malli/coercion
                 :parameters {:body {:ticket-url :string}}}}]

     ["/:code" {:coercion reitit.coercion.malli/coercion
                :parameters {:path {:code :string}}}

      ["/ticket"
       ["" {:post {:handler handlers/add-ticket
                   :parameters {:body {:ticket-id :string}}}}]

       ["/:ticket-id" {:parameters {:path {:ticket-id :string}}}
        ["" {:get {:handler handlers/estimate-watch}}]
        ["/results" {:get handlers/estimate-results}]
        ["/estimate" {:get handlers/estimate-view
                      :post handlers/estimate-done}]
        ["/re-estimate" {:post handlers/estimate-again}]
        ["/events" {:get handlers/events-stream-handler}]]]

      ["events" {:get handlers/events-stream-handler}]]]]

   {:data {:muuntaja m/instance
           :middleware [wrap-cookies
                        wrap-session
                        wrap-params
                        wrap-keyword-params
                        common-cookies
                        ;; tap-request-response
                        rrc/coerce-exceptions-middleware
                        rrc/coerce-request-middleware
                        rrc/coerce-response-middleware
                        muuntaja/format-middleware]}}))

(mount/defstate http-server
  :start (http/start-server (ring/ring-handler app-router) {:port (-> config :http :port)})
  :stop (.close http-server))

(comment
  (mount/start)
  (mount/stop)
  )

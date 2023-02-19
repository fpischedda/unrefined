(ns fpsd.routes
  (:require [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.dev.pretty :as pretty]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [com.brunobonacci.mulog :as u]
            [fpsd.configuration :refer [config]]
            [fpsd.refinements.handlers :as handlers]))

(defn add-headers [handler fixed-headers]
  (fn [request]
      (let [response (handler request)]
        (update response :headers (fn [headers]
                                    (if headers
                                      (merge headers fixed-headers)
                                      fixed-headers))))))

(defn request->endpoint-name
  [request]
  (-> request :reitit.core/match :data (get (:request-method request)) :name))

(defn request->log-context
  [request]
  (-> request
      (select-keys [:request-method :path-params :params :uri :cookies])
      (assoc :endpoint-name (request->endpoint-name request))))

(defn wrap-log-context
  [handler]
  (fn [request]
    (u/with-context (request->log-context request)
      (handler request))))

(defn common-cookies [handler]
  (fn [request]
    (let [user-id (-> request :cookies (get "user-id") :value)
          name (-> request :cookies (get "name") :value)]
      (handler (assoc request :common-cookies {:user-id user-id
                                               :name name})))))

(defn create-app []
  (ring/ring-handler
   (ring/router
    [["/" {:get {:no-doc true
                 :handler handlers/index
                 :name :unrefined/index}}]
     ["/assets" {:no-doc true}
      ["/*" (ring/create-resource-handler)]]

     ["/api"
      {:coercion reitit.coercion.spec/coercion
       :middleware [swagger/swagger-feature
                    ;; query-params & form-params
                    parameters/parameters-middleware
                    ;; content-negotiation
                    muuntaja/format-negotiate-middleware
                    ;; encoding response body
                    muuntaja/format-response-middleware
                    ;; exception handling
                    exception/exception-middleware
                    ;; decoding request body
                    muuntaja/format-request-middleware
                    ;; coercing response bodys
                    rrc/coerce-response-middleware
                    ;; coercing request parameters
                    rrc/coerce-request-middleware
                    [add-headers {"Access-Control-Allow-Origin" "*"
                                  "Access-Control-Allow-Methods" "DELETE, GET, POST, PATCH, PUT, OPTIONS"
                                  "Access-Control-Allow-Headers" "*"}]]}

      ["/docs/swagger.json"
       {:get {:no-doc  true
              :swagger {:info {:title "unrefined API"}}
              :handler (swagger/create-swagger-handler)}}]
      
      ["/refine" {:post {:summary "starts a new refinement session with the provided ticket"
                         :parameters {:body {:ticket-url string?}}
                         :responses {200 {:body {:refinement-path string?
                                                 :refinement-code string?
                                                 :ticket-id string?
                                                 :source-ticket-url string?}}}
                         :handler handlers/create-refinement-api
                         :name :unrefined/create-refinement-api}
                  :options {:no-doc true
                            :handler (fn [_] {:status 200})}}]]

     ["/refine" {:no-doc true}
      ["" {:post {:handler handlers/create-refinement
                  :name :unrefined/create-refinement}}]

      ["/:code"

       ["/ticket"
        ["" {:post {:handler handlers/add-ticket
                    :name :unrefined/add-ticket}}]

        ["/:ticket-id"
         ["" {:get {:handler handlers/estimate-watch
                    :name :unrefined/estimate-watch}}]
         ["/results" {:get {:handler handlers/estimate-results
                            :name :unrefined/estimate-results}}]
         ["/estimate" {:get {:handler handlers/estimate-view
                             :name :unrefined/estimate-view}
                       :post {:handler handlers/estimate-done
                              :name :unrefined/estimate-done}}]
         ["/re-estimate" {:post {:handler handlers/estimate-again
                                 :name :unrefined/estimate-again}}]
         ["/events" {:get {:handler handlers/events-stream-handler
                           :name :unrefined/estimate-stream-handler}}]
         ]]

       ["events" {:get handlers/events-stream-handler}]]]]

    {:exception pretty/exception
     :data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware
                         wrap-cookies
                         wrap-params
                         wrap-keyword-params
                         wrap-log-context
                         common-cookies]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/api/docs"
                                           :url  "/api/docs/swagger.json"})
    (ring/create-default-handler))))

(mount/defstate http-server
  :start (do
           (u/start-publisher! (:logging config))
           (http/start-server (create-app) {:port (-> config :http :port)}))
  :stop (.close http-server))

(comment
  
  (do
    (require '[flow-storm.api :as fs-api])
    (fs-api/local-connect))

  (mount/start)
  (mount/stop)
  )

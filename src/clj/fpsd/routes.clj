(ns fpsd.routes
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
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

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get {:handler handlers/index
                 :name :unrefined/index}}]
     ["/assets/*" (ring/create-resource-handler)]

     ["/refine"
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
         ["/permalink" {:get {:handler handlers/render-stored-ticket
                              :name :unrefined/render-stored-ticket}
                        :post {:handler handlers/store-ticket
                               :name :unrefined/store-ticket}}]
         ]]

       ["events" {:get handlers/events-stream-handler}]]]]

    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware
                         wrap-cookies
                         wrap-params
                         wrap-keyword-params
                         wrap-log-context
                         common-cookies]}})))

(comment
  (#'app {:request-method :get
          :uri "/"
          :form-params {:ticket-url "abc"}})
  ,)

(mount/defstate http-server
  :start (do
           (u/start-publisher! (:logging config))
           (http/start-server #'app {:port (-> config :http :port)}))
  :stop (.close http-server))

(comment
  (mount/start)
  (mount/stop)
  )

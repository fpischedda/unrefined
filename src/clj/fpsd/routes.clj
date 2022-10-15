(ns fpsd.routes
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.core :as r]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
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

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get handlers/index}]
     ["/assets/*" (ring/create-resource-handler)]

     ["/refine"
      ["" {:get handlers/index
           :post {:handler handlers/create-refinement
                  :name :unrefined/create-refinement}}]

      ["/:code"

       ["/ticket"
        ["" {:post {:handler handlers/add-ticket
                    :name :unrefined/add-ticket}}]

        ["/:ticket-id"
         ["" {:get {:handler handlers/estimate-watch
                    :name :unrefined/estimate-watch}}]
         ["/results" {:get handlers/estimate-results}]
         ["/estimate" {:get handlers/estimate-view
                       :post handlers/estimate-done}]
         ["/re-estimate" {:post handlers/estimate-again}]
         ["/events" {:get handlers/events-stream-handler}]
         ["/preview" {:get handlers/ticket-preview}]]]

       ["events" {:get handlers/events-stream-handler}]]]]

    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware
                         wrap-cookies
                         wrap-params
                         wrap-keyword-params
                         common-cookies]}})))

(comment
  (#'app {:request-method :post
        :uri "/refine"
        :form-params {:ticket-url "abc"}})
  ,)

(mount/defstate http-server
  :start (http/start-server #'app {:port (-> config :http :port)})
  :stop (.close http-server))

(comment
  (mount/start)
  (mount/stop)
  )

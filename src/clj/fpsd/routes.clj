(ns fpsd.routes
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [manifold.stream :as s]
            [muuntaja.core :as m]
            [aleph.http :as http]
            [mount.core :as mount]
            [portal.api :as portal]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [rum.core :as rum]
            [fpsd.configuration :refer [config]]
            [fpsd.refinements :as refinements]
            [fpsd.views :as views]))

(comment
  (def p (portal/open))
  (add-tap #'portal/submit)

  ,)

(defn create-session
  [request]
  (let [owner-id (-> request :cookies (get "user-id") :value)
        name (-> request :params :name)
        session (refinements/create {} owner-id name)]
    {:headers {:location (str "/refine/" (:code session))}
     :cookies {"owner" "1"
               "name" name}
     :status 302}))

(defn join-session
  [request]
  (let [code (-> request :params :session-code)
        name (-> request :params :name)
        user-id (-> request :cookies (get "user-id") :value)]
    (if (refinements/details code)
      (do
        (refinements/add-participant code user-id name)
        {:headers {:location (str "/refine/" code)}
         :cookies {"name" name}
         :status 302})
      {:headers {:location "/"}
       :status 302})))

(defn create-or-join
  [request]
  (tap> "create-or-join")
  (tap> request)
  (if (some? (-> request :params :start-session))
    (create-session request)
    (join-session request)))

(defn add-ticket
  [request]
  (tap> "add-ticket")
  (tap> request)
  (let [code (-> request :path-params :code)
        ticket-id (-> request :params :ticket-id)]
    (refinements/add-ticket code ticket-id)
    {:headers {:location (str "/refine/" code)}
     :status 302}))

(defn vote-ticket
  [request]
  (tap> "vote-ticket")
  (tap> request)
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        user-id (-> request :cookies (get "user-id") :value)
        vote (-> request :params :vote int)]
    (refinements/vote-ticket code ticket-id user-id vote)
    {:headers {:location (str "/refine/" code)}
     :status 302}))

(defn refinement-details [request]
  {:body {:refinements (refinements/details (-> request :path-params :code))}})

(defn index
  [request]
  (let [user-id (-> request :cookies (get "user-id") :value)
        name (-> request :cookies (get "name") :value)]
    {:body (rum/render-static-markup (views/index name))
     :headers {:content-type "text/html"}
     :cookies {"user-id" (or user-id (str (random-uuid)))}}))

(defn join
  [request]
  (let [user-id (-> request :cookies (get "user-id") :value)
        name (-> request :cookies (get "name") :value)
        code (-> request :path-params :code)]
    {:body (rum/render-static-markup (views/join name code))
     :headers {:content-type "text/html"}
     :cookies {"user-id" (or user-id (str (random-uuid)))}}))

(defn refinement-page
  [request]
  (let [owner (-> request :cookies (get "owner") :value)
        name (-> request :cookies (get "name") :value)
        session (refinements/details (-> request :path-params :code))]
    {:body (rum/render-static-markup (views/refinement-page session name owner))
     :headers {:content-type "text/html"}
     :status 200}))

(def test-stream (atom nil))
(comment
  (s/put! @test-stream (str "data: {\"k\": 1}\n\n"))
  (s/put! (:event-sink (refinements/details "UZFMVJ")) (str "data: {\"k\": 1}\n\n"))
  ,)

(defn events-handler
  [{:keys [cookies path-params] :as _request}]
  (let [user-id (-> cookies (get "user-id") :value)
        name (-> cookies (get "name") :value)
        code (:code path-params)
        _ (refinements/send-event code {:event "user-joined"
                                        :payload {:username name}})
        events-stream (refinements/user-connected code user-id)]
    (reset! test-stream events-stream)

    {:status 200
     :headers {:content-type "text/event-stream"}
     :body events-stream}))

(defn add-headers [handler fixed-headers]
  (fn [request]
      (let [response (handler request)]
        (update response :headers (fn [headers]
                                    (if headers
                                      (merge headers fixed-headers)
                                      fixed-headers))))))

(def handler
  (ring/ring-handler
   (ring/router
    [["/api" {:options (fn [_] {:headers {"Access-Control-Allow-Origin" "*"}})
              :middleware [[add-headers {"Access-Control-Allow-Origin" "*"}]]}

      ["/refine/:code/ticket/:ticket/vote" {:post vote-ticket}]
      ["/refine/:code/ticket" {:post add-ticket}]
      ["/refine/:code" {:get refinement-details}]
      ["/refine" {:post create-session}]]

     ["/assets/*" (ring/create-resource-handler)]
     ["/" {:get index}]
     ["/events/:code" {:get events-handler}]
     ["/join/:code" {:get join}]
     ["/refine/:code/add-ticket" {:post add-ticket}]
     ["/refine/:code" {:get refinement-page}]
     ["/refine" {:post create-or-join}]]
    {:data {:muuntaja m/instance
            :middleware [wrap-cookies
                         wrap-session
                         wrap-params
                         wrap-keyword-params
                         muuntaja/format-middleware]}})))

(mount/defstate http-server
  :start (http/start-server #'handler {:port (-> config :http :port)})
  :stop (.close http-server))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(comment
  (start)
  (stop)
  )

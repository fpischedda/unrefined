(ns fpsd.refinements.handlers
  (:require
   [rum.core :as rum]
   [fpsd.estimator :as estimator]
   [fpsd.refinements :as refinements]
   [fpsd.views :as views]))

(defn events-stream-handler
  [{:keys [cookies path-params] :as _request}]
  (let [user-id (-> cookies (get "user-id") :value)
        code (:code path-params)
        events-stream (refinements/user-connected code user-id)]
    (reset! test-stream events-stream)

    {:status 200
     :headers {:content-type "text/event-stream"}
     :body events-stream}))

(defn create-refinement
  [request]
  (let [owner-id (-> request :common-cookies (:user-id (str (random-uuid))))
        ticket-id (-> request :params :ticket-id)
        refinement (refinements/create! owner-id {})
        _ticket (refinements/add-ticket (:code refinement) ticket-id)]
    {:headers {:location (format "/refine/%s/ticket/%s" (:code refinement) ticket-id)}
     :cookies {"user-id" owner-id}
     :status 302}))

(defn add-ticket
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :params :ticket-id)]
    (refinements/add-ticket code ticket-id)
    {:headers {:location (format "/refine/%s/ticket/%s" code ticket-id)}
     :status 302}))

(defn parse-int
  "Return the integer value represented by the string int-str
   or nil if it is not possible to parse the number"
  [int-str]
  (try (Integer/parseInt int-str)
       (catch NumberFormatException _ nil)))

(defn vote-ticket
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        user-id (-> request :cookies (get "user-id") :value)
        vote (-> request :params :vote parse-int)]
    (if vote
      (refinements/vote-ticket code ticket-id user-id vote)
      (refinements/skip-ticket code ticket-id user-id))
    {:status 201}))

(defn index
  [request]
  (let [user-id (-> request :common-cookies :user-id)]
    {:body (rum/render-static-markup (views/index))
     :headers {:content-type "text/html"}
     :cookies {"user-id" (or user-id (str (random-uuid)))}}))

(defn estimate-watch
  [request]
  (let [refinement (refinements/details (-> request :path-params :code))
        ticket-id (-> request :path-params :ticket-id)]
    {:body
     (rum/render-static-markup (views/estimate-watch refinement ticket-id))
     :headers {:content-type "text/html"}
     :status 200}))

(defn estimate-reveal
  [request]
  (let [refinement (refinements/details (-> request :path-params :code))
        ticket-id (-> request :path-params :ticket-id)
        ticket (-> refinement :tickets (get ticket-id))
        estimation (estimator/estimate ticket (:settings refinement))]
    {:body
     (rum/render-static-markup (views/estimate-reveal refinement ticket estimation))
     :headers {:content-type "text/html"}
     :status 200}))

(defn estimate-view
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        name (-> request :common-cookies :name)
        code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        refinement (refinements/details code)]

    (if-let [ticket (-> refinement :tickets (get ticket-id))]
      {:body (rum/render-static-markup (views/estimate-view code ticket name))
       :headers {:content-type "text/html"}
       :cookies {"user-id" user-id}
       :status 200}
      {:headers {:location "/"}
       :status 302})))

(defn estimate-done
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        name (-> request :params :name)
        code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        refinement (refinements/details code)
        vote (-> request :params :vote parse-int)]
    (if-let [ticket (-> refinement :tickets (get ticket-id))]
      (do
        (refinements/set-participant code user-id name)
        (if vote
          (refinements/vote-ticket code ticket-id user-id vote)
          (refinements/skip-ticket code ticket-id user-id))
        {:body (rum/render-static-markup (views/estimate-done code ticket name))
         :headers {:content-type "text/html"}
         :cookies {"user-id" user-id
                   "name" name}
         :status 200})
      {:headers {:location "/"}
       :status 302})))

(def test-stream (atom nil))
(comment
  (s/put! @test-stream (str "data: {\"k\": 1}\n\n"))
  (s/put! (:event-sink (refinements/details "UZFMVJ")) (str "data: {\"k\": 1}\n\n"))
  ,)

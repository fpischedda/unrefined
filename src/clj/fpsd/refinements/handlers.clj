(ns fpsd.refinements.handlers
  (:require
   [rum.core :as rum]
   [selmer.parser :refer [render-file]]
   [fpsd.configuration :refer [config]]
   [fpsd.estimator :as estimator]
   [fpsd.refinements :as refinements]
   [fpsd.views :as views]))

(def test-stream (atom nil))
(comment
  (s/put! @test-stream (str "data: {\"k\": 1}\n\n"))
  (s/put! (:event-sink (refinements/details "UZFMVJ")) (str "data: {\"k\": 1}\n\n"))
  ,)

(defn events-stream-handler
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        events-stream (refinements/user-connected code)]
    (reset! test-stream events-stream)
    (when ticket-id
      (refinements/send-ticket-status-event! code ticket-id))

    {:status 200
     :headers {:content-type "text/event-stream"
               :cache-control "no-cache"
               "X-Accel-Buffering" "no"}
     :body events-stream}))

(defn create-refinement
  [request]
  (let [owner-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        ticket-id (-> request :params :ticket-id)
        refinement (refinements/create! owner-id {})
        _ticket (refinements/add-new-ticket! (:code refinement) ticket-id)]
    {:headers {:location (format "/refine/%s/ticket/%s" (:code refinement) ticket-id)}
     :cookies {"user-id" {:value owner-id :same-site :strict}}
     :status 302}))

(defn add-ticket
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :params :ticket-id)]
    (refinements/add-new-ticket! code ticket-id)
    {:headers {:location (format "/refine/%s/ticket/%s" code ticket-id)}
     :status 302}))

(defn parse-int
  "Return the integer value represented by the string int-str
   or nil if it is not possible to parse the number"
  [int-str]
  (try (Integer/parseInt int-str)
       (catch NumberFormatException _ nil)))

(defn get-vote-from-params
  [params]
  (reduce (fn [acc item]
            (if-let [value (get params item)]
              (assoc acc item value)
              acc))
          {:points (-> params :points parse-int)
           :name (or (-> params :name) "Anonymous Coward")}
          [:implementation :tests :risk :pain]))

(defn vote-ticket
  "kind of REST api ready handler...still here just because"
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        user-id (-> request :common-cookies :user-id)
        vote (-> request :params get-vote-from-params)]
    (if (:vote vote)
      (refinements/vote-ticket code ticket-id user-id vote)
      (refinements/skip-ticket code ticket-id user-id))
    {:status 201}))

(defn index
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        ctx {:project-title (:project-title config)}]
    {:body (render-file "templates/index.html" ctx)
     :headers {:content-type "text/html"}
     :cookies {"user-id" {:value user-id
                          :same-site :strict}}}))

(defn estimate-watch
  [request]
  (let [refinement (refinements/details (-> request :path-params :code))
        ticket-id (-> request :path-params :ticket-id)
        ticket (-> refinement :tickets (get ticket-id))]
    {:body
     (render-file "templates/estimate-watch.html" {:refinement refinement
                                                   :ticket ticket})
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
      {:body (render-file "templates/estimate-view.html" {:refinement refinement
                                                          :ticket ticket
                                                          :name name})
       :headers {:content-type "text/html"}
       :cookies {"user-id" {:value user-id :same-site :strict}}
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
        vote (-> request :params get-vote-from-params)]
    (refinements/set-participant code user-id name)
    (if vote
      (refinements/vote-ticket code ticket-id user-id vote)
      (refinements/skip-ticket code ticket-id user-id))
    {:body (render-file "templates/estimate-done.html" {:refinement refinement
                                                        :ticket (-> refinement :tickets (get ticket-id))
                                                        :name name
                                                        :vote (or vote "Skipped")})
     :headers {:content-type "text/html"}
     :cookies {"user-id" {:value user-id :same-site :strict}
               "name" {:value name :same-site :strict}}
     :status 200}))

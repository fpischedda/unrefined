(ns fpsd.refinements.handlers
  (:require
   [portal.api :as portal]
   [selmer.parser :refer [render-file]]
   [fpsd.configuration :refer [config]]
   [fpsd.estimator :as estimator]
   [fpsd.refinements :as refinements]
   [fpsd.refinements.events :as events]
   [fpsd.refinements.helpers :as helpers]))

(comment
  (def p (portal/open))
  (add-tap #'portal/submit)

  (portal/close p)
  ,)

(defn events-stream-handler
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        events-stream (events/user-connected! code)]

    (events/send-event! code {:event "ping"})

    (when ticket-id
      (events/send-ticket-status-event! code (refinements/ticket-details code ticket-id)))

    {:status 200
     :headers {:content-type "text/event-stream"
               :cache-control "no-cache"
               "X-Accel-Buffering" "no"}
     :body events-stream}))

(defn create-refinement
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        ticket-url (-> request :params :ticket-url)
        ticket-id (or (helpers/extract-ticket-id-from-url ticket-url) ticket-url)
        refinement (refinements/create!)
        _ticket (refinements/add-new-ticket!
                 (:code refinement) ticket-id ticket-url)
        _ (events/create-refinement-sink! (:code refinement))]

    {:headers {:location (format "/refine/%s/ticket/%s" (:code refinement) ticket-id)}
     :cookies {"user-id" {:value user-id :same-site :strict}}
     :status 302}))

(defn add-ticket
  [request]
  (let [code (-> request :path-params :code)
        ticket-url (-> request :params :ticket-url)
        ticket-id (extract-ticket-id-from-url ticket-url)]

    (refinements/add-new-ticket! code ticket-id ticket-url)

    (events/send-ticket-added-event! code ticket-id)

    {:headers {:location (format "/refine/%s/ticket/%s" code ticket-id)}
     :status 302}))

(defn index
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))]

    {:body (render-file "templates/index.html")
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

(defn estimate-results
  [request]
  (let [refinement (refinements/details (-> request :path-params :code))
        ticket-id (-> request :path-params :ticket-id)
        ticket (-> refinement :tickets (get ticket-id))
        estimation (estimator/estimate ticket (:settings refinement))]

    {:body
     (render-file "templates/estimate-results.html" {:refinement refinement
                                                     :ticket ticket
                                                     :estimation estimation})
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
       :cookies {"user-id" {:value user-id :same-site :strict}
                 "name" {:value name :same-site :strict}}
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
        skipped (some? (-> request :params :skip-button))
        vote (when-not skipped (-> request :params helpers/get-vote-from-params))]

    (if skipped
      (do
        (refinements/skip-ticket code ticket-id user-id)
        (events/send-vote-event! :user-skipped code user-id ticket-id))
      (do
        (refinements/vote-ticket code ticket-id user-id vote)
        (events/send-vote-event! :user-voted code user-id ticket-id)))

    {:body (render-file "templates/estimate-done.html" {:refinement refinement
                                                        :ticket (-> refinement :tickets (get ticket-id))
                                                        :name name
                                                        :skipped skipped
                                                        :vote vote})
     :headers {:content-type "text/html"}
     :cookies {"user-id" {:value user-id :same-site :strict}
               "name" {:value name :same-site :strict}}
     :status 200}))

(defn estimate-again
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)]

    (refinements/re-estimate-ticket code ticket-id)
    (events/send-re-estimate-event! code ticket-id)

    {:headers {:location (format "/refine/%s/ticket/%s" code ticket-id)}
     :status 302}))

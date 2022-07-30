(ns fpsd.refinements.handlers
  (:require
   [portal.api :as portal]
   [selmer.parser :refer [render-file]]
   [fpsd.configuration :refer [config]]
   [fpsd.estimator :as estimator]
   [fpsd.refinements :as refinements]
   [fpsd.refinements.events :as events]))

(comment
  (def p (portal/open))
  (add-tap #'portal/submit)

  (portal/close p)
  ,)

(defn events-stream-handler
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        events-stream (refinements/user-connected code)]

    (events/send-event! code {:event "ping"})

    (when ticket-id
      (events/send-ticket-status-event! code ticket-id))

    {:status 200
     :headers {:content-type "text/event-stream"
               :cache-control "no-cache"
               "X-Accel-Buffering" "no"}
     :body events-stream}))

(def jira-re #"https://.*/browse/(.*)")

(defn extract-ticket-id-from-url
  [url]
  (if-let [m (re-matches jira-re url)]
    (second m)
    url))

(comment
  (extract-ticket-id-from-url "https://cargo-one.atlassian.net/browse/PE-1234") ;; => "PE-1234"
  ,)

(defn create-refinement
  [request]
  (let [owner-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        ticket-url (-> request :params :ticket-url)
        ticket-id (extract-ticket-id-from-url ticket-url)
        refinement (refinements/create! owner-id {})
        _ticket (refinements/add-new-ticket!
                 (:code refinement) ticket-id ticket-url)]

    {:headers {:location (format "/refine/%s/ticket/%s" (:code refinement) ticket-id)}
     :cookies {"user-id" {:value owner-id :same-site :strict}}
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

(defn parse-int
  "Return the integer value represented by the string int-str
   or nil if it is not possible to parse the number"
  [int-str]
  (try (Integer/parseInt int-str)
       (catch NumberFormatException _ nil)))

(defn get-vote-from-params
  [params]
  {:points (-> params :points parse-int)
   :name (or (-> params :name) "Anonymous Coward")
   :breakdown
   (reduce (fn [acc item]
             (if-let [value (get params item)]
               (assoc acc item value)
               acc))
           {}
           [:implementation :tests :migrations :refactoring :risk :pain])})

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
        skipped (some? (-> request :params :skip-button))
        vote (when-not skipped (-> request :params get-vote-from-params))]

    (refinements/set-participant code user-id name)

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

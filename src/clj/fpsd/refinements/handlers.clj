(ns fpsd.refinements.handlers
  (:require
   [selmer.parser :refer [render-file]]
   [fpsd.estimator :as estimator]
   [fpsd.refinements.core :as core]
   [fpsd.refinements.helpers :as helpers]))

(defn cookie-value
  [value]
  {:value value :same-site :strict :path "/"})

(defn events-stream-handler
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)]

    (if-let [events-stream (core/user-connected! code ticket-id)]
      {:status 200
       :headers {:content-type "text/event-stream"
                 :cache-control "no-cache"
                 "X-Accel-Buffering" "no"}
       :body events-stream}
      {:status 404})))

(defn safe-ticket-url
  [code ticket-id]
  (format "/refine/%s/ticket/%s" code ticket-id))

(defn create-refinement
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        ticket-url (-> request :params :ticket-url)
        {:keys [code ticket-id]} (core/create-refinement ticket-url)]

    {:headers {:location (safe-ticket-url code ticket-id)}
     :cookies {"user-id" (cookie-value user-id)}
     :status 302}))

(defn add-ticket
  [request]
  (let [code (-> request :path-params :code)
        ticket-url (-> request :params :ticket-url)
        ticket (core/add-ticket code ticket-url)]

    {:headers {:location (safe-ticket-url code (:id ticket))}
     :status 302}))

(defn index
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))]

    {:body (render-file "templates/index.html" {})
     :headers {:content-type "text/html"}
     :cookies {"user-id" (cookie-value user-id)}}))

(defn refinement-or-ticket-not-found-error
  []
  {:status 404
       :headers {:content-type "text/html"}
       :body (render-file "templates/index.html"
                          {:error "Unable to find rifenement session or ticket"})})

(defn estimate-watch
  [request]
  (let [refinement (core/get-refinement (-> request :path-params :code))
        ticket-id (-> request :path-params :ticket-id)]

    (if-let [ticket (core/get-refinement-ticket refinement ticket-id)]
      {:body
       (render-file "templates/estimate-watch.html" {:refinement refinement
                                                     :ticket ticket})
       :headers {:content-type "text/html"}
       :status 200}
      (refinement-or-ticket-not-found-error))))

(defn estimate-results
  [request]
  (let [refinement (core/get-refinement (-> request :path-params :code))
        ticket-id (-> request :path-params :ticket-id)]
    (if-let [ticket (core/get-refinement-ticket refinement ticket-id)]
      {:body
       (render-file
        "templates/estimate-results.html"
        {:refinement refinement
         :ticket ticket
         :estimation (estimator/estimate ticket (:settings refinement))})
       :headers {:content-type "text/html"}
       :status 200}
      (refinement-or-ticket-not-found-error))))

(defn estimate-view
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        name (-> request :common-cookies :name)
        code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        refinement (core/get-refinement code)]

    (if-let [ticket (core/get-refinement-ticket refinement ticket-id)]
      {:body (render-file "templates/estimate-view.html" {:refinement refinement
                                                          :ticket ticket
                                                          :name name})
       :headers {:content-type "text/html"}
       :cookies {"user-id" (cookie-value user-id)
                 "name" (cookie-value name)}
       :status 200}
      (refinement-or-ticket-not-found-error))))

(defn estimate-done
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        name (-> request :params :name)
        code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        refinement (core/get-refinement code)
        skipped (some? (-> request :params :skip-button))
        vote (when-not skipped (-> request :params helpers/get-vote-from-params))]

    (core/vote-ticket code ticket-id user-id skipped vote)

    {:body (render-file "templates/estimate-done.html"
                        {:refinement refinement
                         :ticket (core/get-refinement-ticket refinement ticket-id)
                         :name name
                         :skipped skipped
                         :vote vote})
     :headers {:content-type "text/html"}
     :cookies {"user-id" (cookie-value user-id)
               "name" (cookie-value name)}
     :status 200}))

(defn estimate-again
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)]

    (core/re-estimate-ticket code ticket-id)

    {:headers {:location (safe-ticket-url code ticket-id)}
     :status 302}))

(defn ticket-preview
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        preview-future (core/ticket-preview code ticket-id)]

    {:body (render-file "templates/ticket-preview.html" @preview-future)
     :status 200}))

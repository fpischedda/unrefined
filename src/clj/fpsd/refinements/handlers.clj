(ns fpsd.refinements.handlers
  (:require
   [com.brunobonacci.mulog :as u]
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
      (do
        (u/log ::events-stream-handler :refinement code :ticket-id ticket-id)
        {:status 200
         :headers {:content-type "text/event-stream"
                   :cache-control "no-cache"
                   "X-Accel-Buffering" "no"}
         :body events-stream})
      (do
        (u/log ::events-stream-handler
               :refinement code
               :ticket-id ticket-id
               :error "Could not find requested ticket or refinement session")
        {:status 404}))))

(defn safe-ticket-url
  [code ticket-id]
  (format "/refine/%s/ticket/%s" code ticket-id))

(defn create-refinement
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        ticket-url (-> request :params :ticket-url)
        {:keys [code ticket-id]} (core/create-refinement ticket-url)]

    (u/log ::create-refinement :user-id user-id :ticket-url ticket-url :refinement code :ticket-id ticket-id)

    {:headers {:location (safe-ticket-url code ticket-id)}
     :cookies {"user-id" (cookie-value user-id)}
     :status 302}))

(defn add-ticket
  [request]
  (let [code (-> request :path-params :code)
        ticket-url (-> request :params :ticket-url)
        ticket (core/add-ticket code ticket-url)]

    (u/log ::add-ticket
           :ticket-url ticket-url
           :refinement code
           :ticket-id (:id ticket))
    {:headers {:location (safe-ticket-url code (:id ticket))}
     :status 302}))

(defn index
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))]

    {:body (render-file "templates/index.html" {})
     :headers {:content-type "text/html"}
     :cookies {"user-id" (cookie-value user-id)}}))

(defn refinement-or-ticket-not-found-error
  [code ticket-id]
  (u/log ::refinement-or-ticket-not-found-error
         :refinement code
         :ticket-id ticket-id
         :error "Could not find requested ticket or refinement session")
  {:status 404
       :headers {:content-type "text/html"}
       :body (render-file "templates/index.html"
                          {:error "Unable to find rifenement session or ticket"})})

(defn estimate-watch
  [request]
  (let [code (-> request :path-params :code)
        refinement (core/get-refinement code)
        ticket-id (-> request :path-params :ticket-id)]

    (if-let [ticket (core/get-refinement-ticket refinement ticket-id)]
      {:body
       (render-file "templates/estimate-watch.html" {:refinement refinement
                                                     :ticket ticket})
       :headers {:content-type "text/html"}
       :status 200}
      (refinement-or-ticket-not-found-error code ticket-id))))

(defn estimate-results
  [request]
  (let [code (-> request :path-params :code)
        refinement (core/get-refinement code)
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
      (refinement-or-ticket-not-found-error code ticket-id))))

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
      (refinement-or-ticket-not-found-error code ticket-id))))

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

(defn store-ticket
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        error (core/store-ticket code ticket-id)]

    (u/log ::store-ticket :refinement code :ticket ticket-id :error error)
    (if error
      {:status 500
       :body error}
      {:status 201})))

(defn render-stored-ticket
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        data (core/get-stored-ticket code ticket-id)]

    (u/log ::get-stored-ticket
           :refinement code
           :ticket ticket-id
           :refinement-data (:refinement data)
           :ticket-data (:ticket data)
           :estimation-data (:estimation data)
           :error (:error data))

    {:status 200
     :body (render-file "templates/estimate-permalink.html" data)}))

(defn ticket-preview
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        preview-future (core/ticket-preview code ticket-id)]

    {:body (render-file "templates/ticket-preview.html" @preview-future)
     :status 200}))

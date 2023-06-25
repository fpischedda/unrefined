(ns one.unrefined.handlers
  (:require
   [com.brunobonacci.mulog :as u]
   [selmer.parser :refer [render-file]]
   [one.unrefined.estimator :as estimator]
   [one.unrefined.refinements.core :as core]
   [one.unrefined.refinements.helpers :as helpers]))

(defn cookie-value
  [value]
  {:value value :same-site :strict :path "/"})

(defn events-stream-handler
  [request]
  (let [code (-> request :path-params :code)
        ticket-id (-> request :path-params :ticket-id)
        {:keys [stream error]} (core/user-connected! code ticket-id)]

    (if stream
      (do
        (u/log ::events-stream-handler :refinement code :ticket-id ticket-id)
        {:status 200
         :headers {:content-type "text/event-stream"
                   :cache-control "no-cache"
                   "X-Accel-Buffering" "no"}
         :body stream})
      (do
        (u/log ::events-stream-handler
               :refinement code
               :ticket-id ticket-id
               :error error
               :message "Could not find requested ticket or refinement session")
        {:status 404}))))

(defn safe-ticket-url
  [code ticket-id]
  (format "/refine/%s/ticket/%s" code ticket-id))

(defn create-refinement
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        {:keys [ticket-url cheatsheet]} (-> request :params)
        {:keys [code ticket-id]} (core/create-refinement! ticket-url
                                                          {:cheatsheet cheatsheet})]

    (u/log ::create-refinement
           :user-id user-id
           :ticket-url ticket-url
           :refinement code
           :ticket-id ticket-id
           :cheatsheet cheatsheet)

    {:headers {:location (safe-ticket-url code ticket-id)}
     :cookies {"user-id" (cookie-value user-id)}
     :status 302}))

(defn create-refinement-api
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        ticket-url (-> request :body-params :ticket-url)
        {:keys [code ticket-id]} (core/create-refinement! ticket-url
                                                          {:cheatsheet "default"})
        ]

    (u/log ::create-refinement :user-id user-id :ticket-url ticket-url :refinement code :ticket-id ticket-id)

    {:body {:refinement-path (safe-ticket-url code ticket-id)
            :refinement-code code
            :ticket-id ticket-id
            :source-ticket-url ticket-url}
     :cookies {"user-id" (cookie-value user-id)}
     :status 201}))

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

(defn add-ticket-api
  [request]
  (let [code (-> request :path-params :code)
        ticket-url (-> request :body-params :ticket-url)
        ticket (core/add-ticket code ticket-url)
        ticket-id (:id ticket)]

    (u/log ::add-ticket-api
           :ticket-url ticket-url
           :refinement code
           :ticket-id ticket-id)
    {:status 201
     :body {:refinement-path (safe-ticket-url code ticket-id)
            :refinement-code code
            :ticket-id ticket-id
            :source-ticket-url ticket-url}}))

(defn index
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))]
    {:body (render-file "templates/index.html"
                        {:cheatsheets helpers/cheatsheet-map_})
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
  (let [{:keys [code ticket-id]} (:path-params request)
        {:keys [error refinement ticket]} (core/get-refinement-ticket code ticket-id)]

    (if (and refinement ticket)
      {:body
       (render-file "templates/estimate-watch.html" {:refinement refinement
                                                     :ticket ticket})
       :headers {:content-type "text/html"}
       :status 200}
      (do
        (u/log ::estimate-watch
               :message (format "Unable to find refinement %s or ticket %s" code ticket-id)
               :error error)
        (refinement-or-ticket-not-found-error code ticket-id)))))

(defn estimate-results
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        {:keys [error refinement ticket]} (core/get-refinement-ticket code ticket-id)]
    (if (and refinement ticket)
      {:body
       (render-file
        "templates/estimate-results.html"
        {:refinement refinement
         :ticket ticket
         :estimation (estimator/estimate ticket (:settings refinement))})
       :headers {:content-type "text/html"}
       :status 200}
      (do
        (u/log ::estimate-result
               :message (format "Unable to find refinement %s or ticket %s" code ticket-id)
               :error error)
        (refinement-or-ticket-not-found-error code ticket-id)))))

(defn estimate-results-api
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        {:keys [error refinement ticket]} (core/get-refinement-ticket code ticket-id)]
    (if (and refinement ticket)
      {:body
       {:refinement refinement
        :ticket ticket
        :estimation (estimator/estimate ticket (:settings refinement))}
       :headers {:content-type "application/json"}
       :status 200}
      (do
        (u/log ::estimate-result
               :message (format "Unable to find refinement %s or ticket %s" code ticket-id)
               :error error)
        {:status 404}))))

(defn get-user-ticket-estimation
  [ticket user-id]
  (->> ticket
      :sessions
      last
      :votes
      (filter #(= user-id (:author-id %)))
      first))

(comment
  (let [{:keys [error refinement ticket]} (core/get-refinement-ticket "wocmokJqeEvPQ1F37FoPU" "asdf")]
    (get-user-ticket-estimation ticket  "d44524ff-898f-43d9-8ea2-383b98bdff41"))
  ,)

(defn estimate-view
  "Handler for the engineers estimation page."
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        name (-> request :common-cookies :name)
        {:keys [code ticket-id]} (:path-params request)
        {:keys [error refinement ticket]} (core/get-refinement-ticket code ticket-id)]

    (if (and refinement ticket)
      (let [body
            (if-let [estimation (get-user-ticket-estimation ticket user-id)]
              (render-file "templates/estimate-done.html"
                           {:refinement refinement
                            :ticket ticket
                            :estimation estimation})
              (render-file "templates/estimate-view.html"
                           {:refinement refinement
                            :ticket ticket
                            :name name}))]
        {:body body
         :headers {:content-type "text/html"}
         :cookies {"user-id" (cookie-value user-id)
                   "name" (cookie-value name)}
         :status 200})
      (do
        (u/log ::estimate-view
               :message (format "Unable to find refinement %s or ticket %s" code ticket-id)
               :error error)
        (refinement-or-ticket-not-found-error code ticket-id)))))

(defn estimate-done
  [request]
  (let [user-id (or (-> request :common-cookies :user-id) (str (random-uuid)))
        {:keys [code ticket-id]} (:path-params request)
        {:keys [error refinement ticket]} (core/get-refinement-ticket code ticket-id)
        session-num (or (-> request :params :session-num helpers/try-parse-int) 0)]
    
    (if (and refinement ticket)
      (let [cheatsheet (-> refinement :settings :cheatsheet)
            estimation
            (-> request
                :params
                (helpers/get-estimation-from-params cheatsheet))
            name (:author-name estimation)]

        (core/estimate-ticket {:code code
                               :ticket-id ticket-id
                               :session-num session-num
                               :author-id user-id
                               :author-name name
                               :estimation estimation})

        {:body (render-file "templates/estimate-done.html"
                            {:refinement refinement
                             :ticket ticket
                             :estimation estimation})
         :headers {:content-type "text/html"}
         :cookies {"user-id" (cookie-value user-id)
                   "name" (cookie-value name)}
         :status 200})
      (do
        (u/log ::estimate-done
               :message (format "Unable to find refinement %s or ticket %s" code ticket-id)
               :error error)
        (refinement-or-ticket-not-found-error code ticket-id)))))

(defn estimate-again
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        {:keys [ticket error]} (core/get-ticket code ticket-id)]

    (if ticket
      (do
        (core/re-estimate-ticket code ticket)
        {:headers {:location (safe-ticket-url code ticket-id)}
         :status 302})

      (do
        (u/log ::estimate-done
               :message (format "Unable to find refinement %s or ticket %s" code ticket-id)
               :error error)
        (refinement-or-ticket-not-found-error code ticket-id)))))

(defn estimate-again-api
  [request]
  (let [{:keys [code ticket-id]} (:path-params request)
        {:keys [ticket error]} (core/get-ticket code ticket-id)]

    (if ticket
      (do
        (core/re-estimate-ticket code ticket)
        {:status 200})

      (do
        (u/log ::estimate-done
               :message (format "Unable to find refinement %s or ticket %s" code ticket-id)
               :error error)
        {:status 404}))))

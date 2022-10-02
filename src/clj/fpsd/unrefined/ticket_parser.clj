(ns fpsd.unrefined.ticket-parser
  (:require [cheshire.core :refer [parse-string]]
            [org.httpkit.client :as http]))

(defn fetch-jira-ticket-data
  [ticket-url]
  (-> ticket-url (http/get {:as :text}) deref :body parse-string))

(defn parse-jira-ticket-data
  [data]
  {:id (get data "id")
   :url (get data "self")
   :description (-> data (get "renderedFields") (get "description"))
   :summary (-> data (get "fields") (get "summary"))})

(defn fetch-jira-ticket
  [ticket-url]
  (-> ticket-url fetch-jira-ticket-data parse-jira-ticket-data))

(comment
  ;; trying with a ticket provided in JIRA's docs
  (fetch-jira-ticket "https://jira.atlassian.com/rest/api/latest/issue/JRA-9?expand=renderedFields")
  ,)

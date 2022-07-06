(ns fpsd.views
  (:require [rum.core :as rum]
            [fpsd.refinements :as refinements]))

(def project-title "Unrefined! (Alpha)")

(rum/defc index
  []
  [:html
   [:head [:title project-title]
    [:meta {:http-equiv "content-type" :content "text/html; charset=utf-8"}]]
   [:body
    [:h2 project-title]
    [:h4 "The refinement tool no one asked for!"]
    [:div
     [:form {:method "POST" :action "/refine"}
      [:p "HI! Please, write the id of the ticket to refine "
       [:input {:name "ticket-id"
                :placeholder "Ticket id here"}]
       " and " [:button {:name "start-session"} "Start!"]]]]]])

(defn render-settings
  [settings]
  [:p "Settings"
   [:ul
    [:li (str "Max delta: " (:max-vote-delta settings))]
    [:li (str "Max rediscussions: " (:max-rediscussions settings))]]])

(rum/defc estimate-watch
  [refinement ticket-id]
  (let [{:keys [code settings tickets]} refinement
        ticket (get tickets ticket-id)
        sessions (:sessions ticket)]
    [:html
     [:head [:title project-title]
      [:script {:src "/assets/sse.js"}]]
     [:body
      [:input {:id "refinement-code"
               :value code
               :type  "hidden"}]
      [:input {:id "ticket-id"
               :value (:id ticket)
               :type  "hidden"}]
      [:h2 project-title]
      [:h4 "The refinement tool no one asked for!"]

      [:p (str "Refinement session code " code)]
      [:div
       [:p "Ticket id: " (:id ticket) ", share the following URL to estimate "
        (format "http://localhost:8080/refine/%s/ticket/%s/estimate" code ticket-id)
        [:p "Current activity"]
        [:p "Total voted: " [:span {:id "total-voted"}
                             (refinements/count-voted ticket)]]
        [:p "Total skipped: " [:span {:id "total-skipped"}
                               (refinements/count-skipped ticket)]]
        (if (empty? sessions)
          [:p "No estimations yet"]
          [:p "Previous estimations"])]
       (render-settings settings)
       ]
      #_[:script {:src "/assets/main.js"}]]]))

(defmulti render-estimation :result)

(defmethod render-estimation :winner
  [estimation]
  [:div
   [:p "Ticket estimated with a score of: " (:vote estimation)]])

(defmethod render-estimation :ex-equo
  [estimation]
  [:div
   [:p "We have a tie!"]
   [:p "Suggested: " (:suggested estimation)]
   [:p "Votes: " (clojure.string/join ", " (:votes estimation))]])

(defmethod render-estimation :discuss
  [estimation]
  [:div
   [:p "Difference too high! Lets discuss"]
   [:p "Highest: " (:highest-vote estimation) ", voters: " (clojure.string/join ", " (:higest-voters estimation))]
   [:p "Lowest: " (:lowest-vote estimation) ", voters: " (clojure.string/join ", " (:lowest-voters estimation))]])

(rum/defc estimate-reveal
  [refinement ticket estimation]
  (let [{:keys [code settings tickets]} refinement
        session (-> ticket :current-session)]
    [:html
     [:head [:title project-title]
      [:script {:src "/assets/sse.js"}]]
     [:body
      [:h2 project-title]
      [:h4 "The refinement tool no one asked for!"]

      [:p (str "Refinement session code " code)]
      [:div
       [:p (format "Results of the last voting session for ticket %s" (:id ticket))]
       (render-estimation estimation)
       [:p "Total voted: " (refinements/count-voted ticket)]
       [:p "Total skipped: " (refinements/count-skipped ticket)]
       [:p "Votes" [:ul (for [{:keys [vote count authors]} (:votes estimation)]
                          [:li "Vote " vote " selected " count " times by " (clojure.string/join ", " authors)])]]
       [:p "Skipped by " (clojure.string/join ", " (:skips session))]

       (render-settings settings)
       ]
      #_[:script {:src "/assets/main.js"}]]]))

(defn render-ticket-previous-sessions
  [id sessions]
  (when-not (empty? sessions)
    [:p (str "Previous voting sessions for ticket " id)
     (for [s (reverse sessions)]
       [:ul (for [[name vote] (:votes s)]
              [:li (str name ": " vote)])])]))

(rum/defc estimate-view
  [code {:keys [id sessions] :as _ticket} name]
  [:html
   [:head [:title project-title]]
   [:body
    [:h2 project-title]
    [:h4 "The refinement tool no one asked for!"]
    [:div
     [:form {:method "POST" :action (format "/refine/%s/ticket/%s/estimate" code id)}
      [:input {:type :hidden :name "session-code" :value code}]
      [:input {:type :hidden :name "ticket-id" :value id}]

      [:p "HI " [:input {:name "name"
                         :value name
                         :placeholder "Insert your name here"}] " !"]
      [:p (str "We are estimating ticket " id)

       [:div "Please cast your vote "
        [:input {:type :text :name "vote"}]
        [:button {:name "estimate"} "Estimate!"]]]]]

    (render-ticket-previous-sessions id sessions)]])

(rum/defc estimate-done
  [code {:keys [id sessions] :as _ticket} name]
  [:html
   [:head [:title project-title]]
   [:body
    [:h2 project-title]
    [:h4 "The refinement tool no one asked for!"]
    [:div
     [:p (format "Hi %s! Thank you for estimating the ticket %s." name id)]
     [:p "Wait for a new ticket to estimate, or do whatever you want, I am not your mother..."]]

    (render-ticket-previous-sessions id sessions)]])

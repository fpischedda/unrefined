(ns fpsd.views
  (:require [rum.core :as rum]
            [clojure.string :refer [join]]
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
      [:link {:rel "stylesheet" :href "/assets/style.css"}]
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

      [:p "Refinement session code " [:strong code]]
      [:div
       [:p "Ticket id: " [:strong (:id ticket)] " " [:button {:onclick "copy_estimation_link()"} "Copy link"]

        [:p "Current activity"]
        [:p "Total voted: " [:span {:id "total-voted"}
                             (refinements/count-voted ticket)]]
        [:p "Total skipped: " [:span {:id "total-skipped"}
                               (refinements/count-skipped ticket)]]
        [:button {:onclick "reveal_results()"} "Reveal results"]

        (if (empty? sessions)
          [:p "No previous estimations"]
          [:p "Previous estimations"])]
       (render-settings settings)]
      [:script {:src "/assets/main.js"}]]]))

(defn resolve-participant-names
  [user-ids participants]
  (for [user-id user-ids]
    (get participants user-id)))

(defmulti render-estimation
  (fn [estimation _participants] (:result estimation)))

(defmethod render-estimation :winner
  [estimation _participants]
  [:div
   [:p "Ticket estimated with a score of: " (:vote estimation)]])

(defmethod render-estimation :ex-equo
  [estimation participants]
  [:div
   [:p "We have a tie!"]
   [:p "Suggested: " (:suggested estimation)]
   [:p "Same votes: "
    [:ul (for [[vote authors] (:same-votes estimation)]
           [:li "Story points: " vote ", by " (join ", " (resolve-participant-names authors participants))])]]])

(defmethod render-estimation :discuss
  [estimation participants]
  [:div
   [:p "Difference too high! Lets discuss"]
   [:p "Highest: " (:highest-vote estimation)
    ", voters: "
    (join ", " (resolve-participant-names (:highest-voters estimation) participants))]
   [:p "Lowest: " (:lowest-vote estimation)
    ", voters: "
    (join ", " (resolve-participant-names (:lowest-voters estimation) participants))]])

(rum/defc estimate-reveal
  [refinement ticket estimation]
  (let [{:keys [code settings participants]} refinement
        session (-> ticket :current-session)]
    [:html
     [:head [:title project-title]
      [:script {:src "/assets/sse.js"}]]
     [:body
      [:h2 project-title]
      [:h4 "The refinement tool no one asked for!"]

      [:p (str "Refinement session code " code)]
      [:div
       [:h3 (format "Results of the last voting session for ticket %s" (:id ticket))]
       (render-estimation estimation participants)
       [:h3 "Session stats"]
       [:p "Total voted: " (refinements/count-voted ticket)]
       [:p "Total skipped: " (refinements/count-skipped ticket)]
       [:p "Votes"
        [:ul
         (for [{:keys [vote count authors]} (:votes estimation)]
           [:li "Vote " vote " selected " count " times by "
            (join ", " (resolve-participant-names authors participants))])]]
       [:p "Skipped by " (join ", " (resolve-participant-names (:skips session) participants))]

       (render-settings settings)]
      ]]))

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

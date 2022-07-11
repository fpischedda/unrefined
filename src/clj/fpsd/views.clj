(ns fpsd.views
  (:require [rum.core :as rum]
            [clojure.string :refer [join]]
            [fpsd.configuration :refer [config]]
            [fpsd.refinements :as refinements]))

(rum/defc index
  []
  [:html
   [:head [:title (:project-title config)]
    [:meta {:http-equiv "content-type" :content "text/html; charset=utf-8"}]]
   [:body
    [:h2 (:project-title config)]
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

(defn link-to-ticket
  [ticket-id]
  (format (:link-to-ticket config) ticket-id))

(defn link-to-results
  [code ticket-id]
  (format "/reveal/%s/ticket/%s/reveal" code ticket-id))

(defn ticket-activity
  [ticket]
  [:div
   [:p "Current activity"]
   [:p "Total voted: " [:span {:id "total-voted"}
                        (refinements/count-voted ticket)]]
   [:p "Total skipped: " [:span {:id "total-skipped"}
                          (refinements/count-skipped ticket)]]
   [:div {:id "vote-chart" :width 700 :height 600}]])


(rum/defc estimate-watch
  [refinement ticket-id]
  (let [{:keys [code settings tickets]} refinement
        ticket (get tickets ticket-id)
        sessions (:sessions ticket)]
    [:html
     [:head [:title (:project-title config)]
      [:link {:rel "stylesheet" :href "/assets/css/style.css"}]
      [:script {:src "https://www.gstatic.com/charts/loader.js"}]
      [:script {:src "/assets/sse.js"}]]
     [:body {:data-refinement code :data-ticket ticket-id}
      [:h2 (:project-title config)]
      [:h4 "The refinement tool no one asked for!"]

      [:div
       [:p "Ticket id: " [:a {:href (link-to-ticket ticket-id)} [:strong ticket-id]]
        [:div [:small [:button {:onclick "copy_estimation_link()"} "Copy link to estimation page"]]]

        (ticket-activity ticket)

        [:a {:href (link-to-results code ticket-id)}
         [:button "Reveal results"]]]

       (when (empty? sessions)
         [:p "Previous estimations"])

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
     [:head [:title (:project-title config)]
      [:script {:src "/assets/sse.js"}]]
     [:body
      [:h2 (:project-title config)]
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
       (when (:skips session)
         [:p "Skipped by " (join ", " (resolve-participant-names (:skips session) participants))])

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
   [:head [:title (:project-title config)]]
   [:body {:data-refinement code :data-ticket id}
    [:h2 (:project-title config)]
    [:h4 "The refinement tool no one asked for!"]
    [:div
     [:form {:method "POST" :action (format "/refine/%s/ticket/%s/estimate" code id)}

      [:p "HI " [:input {:name "name"
                         :value name
                         :placeholder "Insert your name here"}] " !"]
      [:p "We are estimating ticket " [:a {:href (link-to-ticket id)} [:string id]]
       [:div [:small [:button {:onclick "copy_estimation_link()"} "Copy link to estimation page"]]]]

      [:p "Please cast your vote "
       [:input {:type :text :name "vote"}]
       [:button {:name "estimate"} "Estimate!"]]]]

    (render-ticket-previous-sessions id sessions)]])

(rum/defc estimate-done
  [code {:keys [id sessions] :as ticket} name]
  [:html
   [:head [:title (:project-title config)]
    [:link {:rel "stylesheet" :href "/assets/css/style.css"}]
    [:script {:src "https://www.gstatic.com/charts/loader.js"}]
    [:script {:src "/assets/sse.js"}]]
   [:body {:data-refinement code :data-ticket id}
    [:h2 (:project-title config)]
    [:h4 "The refinement tool no one asked for!"]
    [:div
     [:p (format "Hi %s! Thank you for estimating the ticket %s." name id)]

     (ticket-activity ticket)

     [:p "Wait for a new ticket to estimate, or do whatever you want, I am not your mother..."]]

    (render-ticket-previous-sessions id sessions)
    [:script {:src "/assets/main.js"}]]])

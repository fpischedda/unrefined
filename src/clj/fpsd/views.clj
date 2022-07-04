(ns fpsd.views
  (:require [rum.core :as rum]
            [fpsd.refinements :as refinements]))

(rum/defc index
  [name]
  [:html
   [:head [:title "Refined! (Alpha)"]]
   [:body
    [:h2 "Refined! (Alpha)"]
    [:div
     [:form {:method "POST" :action "/refine"}
      [:p "HI! Please, write the id of the ticket refine"
       [:input {:name "name"
                         :value name
                         :placeholder "Insert your name here"}] " !"]
      [:p "Start a refinement sessionn " [:button {:name "start-session"} "Now!"]]]]]])

(rum/defc render-session
  [{estimator :estimator :as _session}]
  [:div (case (:result estimator)
          :ex-equo
          [:p "Ex equo"
           [:p (str "Suggested: " (:suggested estimator))]]
          :discuss
          [:p "Discuss"
           [:div (str "Lowest score: " (:lowest-vote estimator) " by " (:lowest-voters estimator))]
           [:div (str "Highest score: " (:min-vote estimator) " by " (:highest-voters estimator))]]
          :winner
          [:p (str "Ticket estimated with a score of " (:vote estimator))])])

(rum/defc render-ticket
  [{:keys [id sessions] :as _ticket} code]
  [:div
   [:h3 "Id: " id]
   [:p "Start voting " [:button {:name "start-voting"
                                 :onclick "toggle_voting(this)"
                                 :data-session-code code
                                 :data-ticket-id id} "tic toc"]]
   [:p "Voting sessions"
    (for [s (reverse sessions)]
      (render-session s))]])

(rum/defc refinement-page
  [refinement name owner]
  (let [{:keys [code settings tickets]} refinement]
    [:html
     [:head [:title "Refined! (Alpha)"]
      [:script {:src "/assets/sse.js"}]]
     [:body
      [:input {:id "refinement-code"
               :value code
               :style {"display" "hidden"}}]
      [:h2 "Refined! (Alpha)"]
      [:h4 "The refinement tool no one asked for!"]
      (if (some? owner)
        [:p (str "Hi " name "! You are the owner of the current session.")]
        [:p (str "Hi " name "! You can vote in the current session.")])
      [:p (str "Session code " code)]
      [:div
       [:p "Settings"
        [:ul
         [:li (str "Max delta: " (:max-vote-delta settings))]
         [:li (str "Max rediscussions: " (:max-rediscussions settings))]]]
       [:p "Add a new ticket "
        [:form {:id "create-ticket-form" :action "javascript: create_ticket(this);"}
         [:input {:name "ticket-id"
                  :id "new-ticket-id"
                  :placeholder "Ticket ID"}]
         [:button {:name "add-ticket"} "+"]]]
       [:p "Tickets" (for [t (reverse tickets)]
                       (render-ticket t code))]]
      [:script {:src "/assets/main.js"}]]]))

(rum/defc join
  [name code]
  [:html
   [:head [:title "Refined! (Alpha)"]]
   [:body
    [:h2 "Refined! (Alpha)"]
    [:h4 "The refinement tool no one asked for!"]
    [:div
     [:form {:method "POST" :action "/refine"}
      [:p "HI " [:input {:name "name"
                         :value name
                         :placeholder "Insert your name here"}] " !"]
      [:p (str "Join the existing session " code " ")
       [:input {type :hidden :name "session-code" :value code}]
       [:button {:name "join-session"} "Join"]]]]]])

(rum/defc estimate-view
  [code {:keys [id sessions] :as _ticket} name]
  [:html
   [:head [:title "Refined! (Alpha)"]]
   [:body
    [:h2 "Refined! (Alpha)"]
    [:h4 "The refinement tool no one asked for!"]
    [:div
     [:form {:method "POST" :action (format "/refine/%s/ticket/%s/vote" code id)}
      [:p "HI " [:input {:name "name"
                         :value name
                         :placeholder "Insert your name here"}] " !"]
      [:p (str "We are estimating ticket " id)
       [:input {type :hidden :name "session-code" :value code}]
       [:input {type :hidden :name "ticket-id" :value id}]

       [:input {type :hidden :name "ticket-id" :value id}]
       [:button {:name "join-session"} "Join"]]]]
    (when-not (empty? sessions)
      [:p (str "Previous voting sessions for ticket " id)
       (for [s (reverse sessions)]
         [:ul (for [[name vote] (:votes s)]
                [:li (str name ": " vote)])])])]])

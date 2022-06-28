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
      [:p "HI " [:input {:name "name"
                         :value name
                         :placeholder "Insert your name here"}] " !"]
      [:p "Start a refinement session " [:button {:name "start-session"} "Now!"]]
      [:p "Or"]
      [:p "Join an existing session "
       [:input {:name "session-code" :placeholder "Session code here"}]
       [:button {:name "join-session"} "Join"]]]]]])

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

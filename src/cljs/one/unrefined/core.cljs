(ns one.unrefined.core
  (:require
   [rum.core :as rum]))

;; At the moment the state of the app is just an atom, maybe it will
;; be enough maybe not, lets see (I'd like to switch to Datascript if
;; future)
(defonce app-state (atom {:name "New cheat sheet"
                          :breakdowns [{:id :implementation
                                        :label "Implementation"
                                        :items ["No change" "One small change"]}
                                       {:id :domain
                                        :label "Domain"
                                        :items ["No change" "Additive change to one domain entity" "Add one domain entity"]}
                                       {:id :migrations
                                        :label "Database migrations"
                                        :items ["No change" "Additive change to one table" "Add one table"]}]}))

(defn remove-by-index
  "Returns a new vec removing the item at index, util to work with breakdowns
   and their items."
  [items index]
  (into (vec (subvec items 0 index))
        (subvec items (inc index))))

(defn delete-breakdown-by-index [index]
  (swap! app-state update-in [:breakdowns] remove-by-index index))

(defn delete-breakdown-item-by-index [breakdown-index item-index]
  (swap! app-state update-in
         [:breakdowns breakdown-index :items]
         remove-by-index item-index))

(defn clean-input-by-id
  [input-field-id]

  (-> js/document
      (.getElementById input-field-id)
      (.-value)
      (set! "")))

(comment
  (clean-input-by-id "breakdown-item-new-2")
  ,)

(defn append-breakdown-item
  "Add an item to an existing breakdown"
  [index]
  (let [element-id (str "breakdown-item-new-" index)
        item (.-value (js/document.getElementById element-id))]
    ;; libraries like citrus or re-frame de-couple rendering and state management
    (swap! app-state update-in [:breakdowns index :items] conj item)
    (clean-input-by-id element-id)))

(rum/defc breakdown-row
  [index {:keys [label items]}]
  [:tr
   [:td [:em label] [:button {:class "btn"
                              :on-click
                              (fn [] (delete-breakdown-by-index index))}
                     [:i {:class "bi bi-x-circle-fill"}]]]
   (map-indexed (fn [idx item]
                  [:td item
                   [:button {:class "btn"
                             :on-click
                             (fn [] (delete-breakdown-item-by-index index idx))}
                    [:i {:class "bi bi-x-circle-fill"}]]]) items)
   [:td
    [:div.input-group
     [:input.input {:type "text"
                    :id (str "breakdown-item-new-" index)
                    :placeholder "Item"}]
     [:button.btn {:on-click (fn [] (append-breakdown-item index))}
      [:i {:class "bi bi-node-plus-fill"}]]]
    ]])

(defn new-breakdown-from-form []
  (let [label (.-value (js/document.getElementById "breakdown"))
        id (keyword label)]
    {:id id
     :label label
     :items ["No change"]}))

(defn add-breakdown
  []
  (swap! app-state update-in [:breakdowns] #(conj % (new-breakdown-from-form)))
  (clean-input-by-id "breakdown"))

(rum/defc cheatsheet
  "Chatsheet main component, it is a container of breakdown items,
   it is possible to add and remove items, it is not possible to edit
   them yet."
  [{:keys [name breakdowns]}]

  [:table
   {:class "table"}
   [:tbody
    [:tr [:td name]]
    (map-indexed breakdown-row breakdowns)
    [:tr 
     [:td
      [:div {:class "input-group"}
       [:input {:class "input" :type "text" :id "breakdown" :placeholder "Breakdown"}]
       [:button {:class "btn btn-primary"
                 :on-click (fn [] (add-breakdown))} "Add"]]]]]])

(rum/defc app < rum/reactive []
  [:div
   [:h1 "Custom cheatsheet"]
   (cheatsheet (rum/react app-state))])

(defonce root (js/document.getElementById "app"))

(defn ^:export init []
  (rum/mount (app) root))

(js/console.log "----")
(init)

 ;; Start after cljs-jack-in:
   ;; shadow.user> (shadow/watch :projectname)
   ;; [:projectname] Configuring build.
   ;; [:projectname] Compiling ...
   ;; :watching[:projectname] Build completed. (114 files, 44 compiled, 0 warnings, 7.43s)
   ;; shadow.user> (shadow/repl :projectname)
   ;; cljs.user> (in-ns 'projectname.core)



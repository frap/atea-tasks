(ns duct2.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :refer [subscribe dispatch]]))


(defn task-input [{:keys [title on-save on-stop]}]
  (let [val (reagent/atom title)
        stop #(do (reset! val "")
                  (when on-stop (on-stop)))
        save #(let [v (-> @val str clojure.string/trim)]
               (when (seq v) (on-save v))
               (stop))]
    (fn [props]
      [:input (merge props
                     {:type "text"
                      :value @val
                      :auto-focus true
                      :on-blur save
                      :on-change #(reset! val (-> % .-target .-value))
                      :on-key-down #(case (.-which %)
                                     13 (save)
                                     27 (stop)
                                     nil)})])))


(defn task-item
  []
  (let [editing (reagent/atom false)]
    (fn [{:keys [id done work tag]}]
      [:li {:class (str (when done "completed ")
                        (when @editing "editing"))}
        [:div.view
          [:input.toggle
            {:type "checkbox"
             :checked done
             :on-change #(dispatch [:toggle-done id])}]
          [:label
            {:on-double-click #(reset! editing true)}
           work]
          [:label
            {:on-double-click #(reset! editing true)}
            tag]
          [:button.destroy
            {:on-click #(dispatch [:delete-task id])}]]
        (when @editing
          [task-input
            :class "edit"
           :work work
           :on-save #(dispatch [:save id %])
           :on-stop #(reset! editing false)]
          [task-input
            :class "edit"
           :tag tag
           :on-save #(dispatch [:save id %])
           :on-stop #(reset! editing false)])])))


(defn task-list
  []
  (let [visible-tasks @(subscribe [:visible-tasks])
        all-complete? @(subscribe [:all-complete?])]
      [:section#main
        [:input#toggle-all
          {:type "checkbox"
           :checked all-complete?
           :on-change #(dispatch [:complete-all-toggle (not all-complete?)])}]
        [:label
          {:for "toggle-all"}
          "Mark ALL as complete"]
        [:ul#task-list
          (for [task  visible-tasks]
            ^{:key (:id task)} [task-item task])]]))


(defn footer-controls
  []
  (let [[active done] @(subscribe [:footer-counts])
        showing       @(subscribe [:showing])
        a-fn          (fn [filter-kw txt]
                        [:a {:class (when (= filter-kw showing) "selected")
                             :href (str "#/" (name filter-kw))} txt])]
    [:footer#footer
     [:span#task-count
      [:strong active] " " (case active 1 "task" "tasks") " left"]
     [:ul#filters
      [:li (a-fn :all    "All")]
      [:li (a-fn :active "Active")]
      [:li (a-fn :done   "Completed")]]
     (when (pos? done)
       [:button#clear-completed {:on-click #(dispatch [:clear-completed])}
        "Clear completed"])]))


(defn task-entry
  []
  [:header#header
    [:h1 "Atea tasks"]
    [task-input
      {:id "new-task"
       :placeholder "What does the Customer need?"
       :on-save #(dispatch [:add-task %])}]])


(defn task-app
  []
  [:div
   [:section#taskapp
    [task-entry]
    (when (seq @(subscribe [:tasks]))
      [task-list])
    [footer-controls]]
   [:footer#info
    [:p "Double-click to edit a task"]]])

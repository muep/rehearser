(ns rehearser.ui.rehearsals.suggestion
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.plan :as plan]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components]))

(defn suggestion-page [{:keys [db url-prefix whoami mistral-api-key]}]
  (let [current (plan/suggestions whoami)
        all-rehearsals (rehearsal-service/find-all db whoami)]
    {:status 200
     :body
     (common-ui/page
      url-prefix whoami "Suggestions"
      [:main
       [:h1 "Exercise Suggestions"]

       (if mistral-api-key
         [:form {:method "post"}
          [:button {:type "submit" :name "action" :value "initiate"}
           "Get Suggestions"]]
         [:p "No Mistral API key configured"])

       (if current
         (if-let [error (:error current)]
           [:div {:class "error"} "Error: " (hiccup/h error)]
           (let [{:keys [suggestions current-rehearsal reference-rehearsal-ids]} current]
             (when current-rehearsal
               [:div
                [:h2 "Current Rehearsal"]
                [:p (hiccup/h (:title current-rehearsal))]

                [:h2 "Reference Rehearsals"]
                [:ul
                 (for [id reference-rehearsal-ids
                       :let [rehearsal (first (filter #(= id (:id %)) all-rehearsals))]]
                   [:li (components/rehearsal-link rehearsal url-prefix)])]

                [:h2 "Suggestions"]
                (if (seq suggestions)
                  [:ul
                   (for [{:keys [id title rationale]} suggestions]
                     [:li
                      [:strong (hiccup/h title)]
                      " - " (hiccup/h rationale)
                      [:form {:action (str url-prefix "/rehearsals/" (:id current-rehearsal) "/new-entry.html")
                              :method "get" :style "display: inline"}
                       [:input {:type "hidden" :name "exercise-id" :value id}]
                       [:button {:type "submit"} "Add"]]])
                   ]
                  [:p "No suggestions available"])])))
         [:p "No suggestions have been generated yet."])])}))

(defn suggestion-initiate! [{{{:keys [action]} :form} :parameters
                          :keys [db url-prefix whoami mistral-api-key]}]
  (when (= action "initiate")
    (let [_ (plan/initiate-suggestions db whoami mistral-api-key)]
      {:status 303
       :headers {"location" (str url-prefix "/rehearsals/suggestions.html")}})))

(def routes
  [["/rehearsals/suggestions.html"
    {:get {:handler suggestion-page}
     :post {:parameters {:form [:map [:action string?]]}
            :handler suggestion-initiate!}}]])

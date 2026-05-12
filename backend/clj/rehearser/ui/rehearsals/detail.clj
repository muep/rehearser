(ns rehearser.ui.rehearsals.detail
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.plan :as plan]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components])
  (:import (java.time Duration Instant)))

(defn rehearsal-page [{{{:keys [id]} :path} :parameters
                       :keys [db url-prefix whoami mistral-api-key]}]
  (if-let [rehearsal (rehearsal-service/find-rehearsal db whoami id)]
    (let [{:keys [start-time duration title description entries]} rehearsal
          now (Instant/now)
          seconds (if duration
                    duration
                    (.getSeconds (Duration/between start-time now)))
          all-rehearsals (rehearsal-service/find-all db whoami)
          suggestions-data (plan/suggestions whoami)]
      {:status 200
       :body
       (common-ui/page
        url-prefix whoami title
        [:main
         [:h1 [:a {:href (str url-prefix "/rehearsals.html")} "Rehearsals"]
          " / " (hiccup/h title)]

         [:p "On " (components/format-instant start-time)]
         (when description
           [:p "Description: " (hiccup/h description)])
         [:p "Duration: " seconds " seconds"]

         (if duration
           [:form {:action (str url-prefix "/rehearsals/" id "/open.html")
                   :method "post"}
            [:button {:type "submit"} "Reopen"]]
           [:form {:action (str url-prefix "/rehearsals/" id "/close.html")
                   :method "post"}
            [:button {:type "submit"} "End rehearsal"]])

         [:h2 "Entries"]
         (when-not duration
           [:a {:href (str url-prefix "/rehearsals/" id "/entry-add-search.html")}
            "Add entry"])

         [:ul
          (for [{:keys [entry-time] :as entry} entries]
            [:li (components/entry-link entry url-prefix)
             " at " (hiccup/h (components/format-time entry-time))])]

         (when mistral-api-key
           [:form {:method "post" :action (str url-prefix "/rehearsals/" id "/initiate-suggestions.html")}
            [:button {:type "submit"} "Refresh Suggestions"]])

         (when suggestions-data
           (if-let [error (:error suggestions-data)]
             [:div {:class "error"} "Error: " (hiccup/h error)]
             (let [{:keys [suggestions current-rehearsal reference-rehearsal-ids]} suggestions-data]
               [:div
                (when (and current-rehearsal (= id (:id current-rehearsal)))
                  [:div
                   [:h2 "Reference Rehearsals"]
                   [:ul
                    (for [ref-id reference-rehearsal-ids
                          :let [ref-rehearsal (first (filter #(= ref-id (:id %)) all-rehearsals))]]
                      [:li (components/rehearsal-link ref-rehearsal url-prefix)])]])

                (when (seq suggestions)
                  [:div
                   [:h2 "Suggestions"]
                   [:ul
                    (for [{:keys [exercise-id title rationale]} suggestions]
                      [:li
                       [:strong (hiccup/h title)]
                       " - " (hiccup/h rationale)
                       [:form {:action (str url-prefix "/rehearsals/" id "/new-entry.html")
                               :method "get" :style "display: inline"}
                        [:input {:type "hidden" :name "exercise-id" :value exercise-id}]
                        [:button {:type "submit"} "Add"]]])]])])))

         [:h2 "Other"]
         [:a {:href (str url-prefix "/rehearsals/" id "/edit.html")} "Edit"]])})
    {:status 404
     :body "Did not find that rehearsal"}))

(defn rehearsal-close! [{{{:keys [id]} :path} :parameters
                          :keys [db url-prefix whoami]}]
  (rehearsal-service/close-rehearsal! db whoami id (Instant/now))
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" id "/rehearsal.html")}})

(defn rehearsal-open! [{{{:keys [id]} :path} :parameters
                        :keys [db url-prefix whoami]}]
  (rehearsal-service/update-rehearsal! db whoami id {:duration nil})
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" id "/rehearsal.html")}})

(defn rehearsal-initiate-suggestions! [{{{:keys [id]} :path} :parameters
                                       :keys [db url-prefix whoami mistral-api-key]}]
  (when mistral-api-key
    (plan/initiate-suggestions db whoami mistral-api-key))
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" id "/rehearsal.html")}})

(def routes
  [["/rehearsals/:id/rehearsal.html"
    {:get {:parameters {:path {:id int?}}}
     :handler rehearsal-page}]
   ["/rehearsals/:id/close.html"
    {:post {:parameters {:path {:id int?}}
            :handler rehearsal-close!}}]
   ["/rehearsals/:id/open.html"
    {:post {:parameters {:path {:id int?}}
            :handler rehearsal-open!}}]
   ["/rehearsals/:id/initiate-suggestions.html"
    {:post {:parameters {:path {:id int?}}
            :handler rehearsal-initiate-suggestions!}}]])

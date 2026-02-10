(ns rehearser.ui.rehearsals.edit
  (:require
   [hiccup.core :as hiccup]
   [hiccup.form :as form]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components]
   [clojure.string :as str])
  (:import
   (java.time Instant Duration)))

(defn parse-instant [s]
  (try
    (Instant/parse s)
    (catch Exception e
      nil)))

(defn parse-duration [s]
  (try
    (when (seq s)
      (Long/parseLong s))
    (catch Exception e
      nil)))

(defn edit-page [{{{:keys [id]} :path} :parameters
                  :keys [db url-prefix whoami] :as req}]
  (if-let [rehearsal (rehearsal-service/find-rehearsal db whoami id)]
    (let [{:keys [title description start-time duration]} rehearsal
          start-time-str (str start-time)
          duration-str (str duration)]
      {:status 200
       :body
       (common-ui/page
        url-prefix whoami (str "Edit: " title)
        [:main
         [:h1 [:a {:href (str url-prefix "/rehearsals.html")} "Rehearsals"]
          " / " [:a {:href (str url-prefix "/rehearsals/"
                                id "/rehearsal.html")} (hiccup/h title)]
          " / Edit"]

         [:form {:action (str url-prefix "/rehearsals/" id "/edit.html")
                 :method "post"}
          [:div
           [:label {:for "title"} "Title:"]
           [:input {:type "text" :name "title" :id "title" :value (hiccup/h title) :required true}]]

          [:div
           [:label {:for "description"} "Description:"]
           [:textarea {:name "description" :id "description" :rows 4}
            (hiccup/h description)]]

          [:div
           [:label {:for "start-time"} "Start Time (ISO-8601 format):"]
           [:input {:type "text" :name "start-time" :id "start-time" :value start-time-str}]]

          [:div
           [:label {:for "duration"} "Duration (seconds, empty for open rehearsal):"]
           [:input {:type "text" :name "duration" :id "duration" :value duration-str}]]

          [:div
           [:button {:type "submit"} "Save Changes"]
           [:a {:href (str url-prefix "/rehearsals/" id "/rehearsal.html")} "Cancel"]]]])})
    {:status 404
     :body "Did not find that rehearsal"}))

(defn edit-save! [{{{:keys [id]} :path
                    {:keys [title description start-time duration]} :form} :parameters
                   :keys [db url-prefix whoami] :as req}]
  (if-let [rehearsal (rehearsal-service/find-rehearsal db whoami id)]
    (let [parsed-start-time (parse-instant start-time)
          parsed-duration (parse-duration duration)
          update-data (cond-> {:title title}
                        description (assoc :description description)
                        parsed-start-time (assoc :start-time parsed-start-time)
                        (nil? parsed-duration) (assoc :duration nil)
                        parsed-duration (assoc :duration parsed-duration))]
      (rehearsal-service/update-rehearsal! db whoami id update-data)
      {:status 303
       :headers {"location" (str url-prefix "/rehearsals/" id "/rehearsal.html")}})
    {:status 404
     :body "Did not find that rehearsal"}))

(def routes
  [["/rehearsals/:id/edit.html"
    {:get {:parameters {:path {:id int?}}
           :handler edit-page}
     :post {:parameters {:path {:id int?}
                         :form [:map
                                [:title string?]
                                [:description {:optional true} string?]
                                [:start-time {:optional true} string?]
                                [:duration {:optional true} string?]]}
            :handler edit-save!}}]])

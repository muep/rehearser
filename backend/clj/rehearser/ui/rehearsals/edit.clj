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
          duration-str (str duration)
          parent-url (str url-prefix "/rehearsals/" id "/rehearsal.html")]
      {:status 200
       :body
       (common-ui/page
        url-prefix whoami (str "Edit: " title)
        [:main
         [:h1 [:a {:href (str url-prefix "/rehearsals.html")} "Rehearsals"]
          " / " [:a {:href parent-url} (hiccup/h title)]
          " / Edit"]

         [:form {:action (str url-prefix "/rehearsals/" id "/edit.html")
                 :method "post"}
          [:div {:class "labeled-input"}
           [:label {:for "title"} "Title"]
           [:input {:type "text" :name "title" :id "title" :value (hiccup/h title) :required true}]]

          [:div {:class "labeled-input"}
           [:label {:for "description"} "Description"]
           [:textarea {:name "description" :id "description" :rows 4}
            (hiccup/h description)]]

          [:div {:class "labeled-input"}
           [:label {:for "start-time"} "Start Time"]
           [:input {:type "text" :name "start-time" :id "start-time" :value start-time-str}]
           [:small "ISO-8601 format"]]

          [:div {:class "labeled-input"}
           [:label {:for "duration"} "Duration"]
           [:input {:type "text" :name "duration" :id "duration" :value duration-str}]
           [:small "Seconds (leave empty for open rehearsal)"]]

          [:div
           [:button {:type "submit"} "Save Changes"]]]])})
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

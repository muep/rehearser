(ns rehearser.ui.rehearsals
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.exercise :as exercise-service]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.service.variant :as variant-service]
   [rehearser.ui.common :as common-ui])
  (:import
   (java.time Duration Instant ZoneId)
   (java.time.format DateTimeFormatter)))

(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
(def time-formatter (DateTimeFormatter/ofPattern "HH:mm"))
(def zone (ZoneId/of "UTC"))

(defn tune-link [{:keys [exercise-id exercise-title]}]
  [:a {:href (str "/tunes/" exercise-id "/tune.html")} (hiccup/h exercise-title)])

(defn entry-link [{:keys [rehearsal-id id exercise-title]}]
  [:a {:href (str "/rehearsals/"
                  rehearsal-id
                  "/entry/"
                  id
                  "/entry.html")} (hiccup/h exercise-title)])

(defn format-instant [instant]
  (.format formatter (.atZone instant zone)))

(defn format-time [instant]
  (.format time-formatter (.atZone instant zone)))

(defn rehearsal-link [{:keys [id title]}]
  [:a {:href (str "/rehearsals/" id "/rehearsal.html")} (hiccup/h title)])

(defn rehearsal-page [{{{:keys [id]} :path} :parameters
                       :keys [db whoami]}]
  (if-let [rehearsal (rehearsal-service/find-rehearsal db whoami id)]
    (let [{:keys [start-time duration title entries]} rehearsal]
      {:status 200
       :body
       (common-ui/page
        whoami title
        [:main
         [:h1 [:a {:href "/rehearsals.html"} "Rehearsals"] " / " (hiccup/h title)]

         [:p "On " (format-instant start-time)]
         (if duration
           [:p "Took " duration " seconds"]
           [:p "Been going on for " (.getSeconds (Duration/between start-time (Instant/now))) " seconds"])

         (when-not duration
           [:a {:href (str "/rehearsals/" id "/new-entry.html")} "Add entry"])

         [:h2 "Entries"]
         [:ul
          (for [{:keys [exercise-title entry-time] :as entry} entries]
            [:li (entry-link entry) " at " (hiccup/h (format-time entry-time))])]])})
    {:status 404
     :body "Did not find that rehearsal"}))

(defn entry-add-page [{{{:keys [rehearsal-id]} :path} :parameters
                       :keys [db whoami]}]
  (let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)
        tunes (exercise-service/find-all db whoami)
        variants (variant-service/find-all db whoami)]
    {:status 200
     :body
     (common-ui/page
      whoami (str (:title rehearsal) " / New entry")
      [:main
       [:h1 (rehearsal-link rehearsal) " / New entry"]

       [:form {:action (str "/rehearsals/" rehearsal-id "/new-entry.html")
               :method "post"}
        [:div
         [:label {:for "exercise-input"} "Tune:"]
         [:select {:id "exercise-input"
                   :name "exercise-id"
                   :required true}
          (for [tune tunes]
            [:option {:value (:id tune)} (hiccup/h (:title tune))])]]
        (if (< 1 (count variants))
          [:div
           [:label {:for "variant-input"} "Instrument:"]
           [:select {:id "variant-input"
                     :name "variant-id"}
            (for [v variants]
              [:option {:value (:id v)} (hiccup/h (:title v))])]]
          [:input {:type "hidden" :name "variant-id" :value (:id (first variants))}])
        [:div
         [:label {:for "remarks-input"} "Remarks"]
         [:textarea {:id "remarks-input"
                     :name "remarks"
                     :placeholder "How did it go?"}]]
        [:input {:type "submit" :value "Submit"}]]])}))

(defn entry-add! [{{{:keys [rehearsal-id]} :path
                    {:keys [remarks exercise-id variant-id]} :form} :parameters
                   :keys [db whoami] :as req}]
  (rehearsal-service/insert-entry! db whoami {:rehearsal-id rehearsal-id
                                              :exercise-id exercise-id
                                              :variant-id variant-id
                                              :entry-time (Instant/now)
                                              :remarks remarks})
  {:status 303
   :headers {"location" (str "/rehearsals/" rehearsal-id "/rehearsal.html")}})

(defn entry-page [{{{:keys [rehearsal-id id]} :path} :parameters
                   :keys [db whoami]}]
  (if-let [[rehearsal entry]
           (let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)
                 entry (->> rehearsal :entries (some #(when (= id (:id %)) %)))]
             (when entry [rehearsal entry]))]
    {:status 200
     :body
     (common-ui/page
      whoami (str (:title rehearsal) " / " (:exercise-title entry))
      [:main
       [:h1 (rehearsal-link rehearsal) " / " (:exercise-title entry)]])}
    {:status 404
     :body (str "No entry " id " in rehearsal " rehearsal-id)}))

(-> (rehearsal-service/find-rehearsal (user/db) {:account-id 1} 5) :entries )

(defn rehearsal-index-page [{:keys [db whoami]}]
  {:status 200
   :body
   (let [[closed-ones open-ones] (->> (rehearsal-service/find-all db whoami)
                                      (group-by :is-open)
                                      sort
                                      (map second))]
     (common-ui/page
      whoami "Rehearsals"
      [:main
       [:h1 "Rehearsals"]

       [:h2 "Current one"]

       (if-let [open-rehearsal (first open-ones)]
         [:span
          (rehearsal-link open-rehearsal)
          " started at "
          (-> open-rehearsal
              :start-time
              format-instant
              hiccup/h)]
         [:form {:method "post"}
          [:p "Nothing ongoing, but a new one may be started"]
          [:div
           [:label {:for "title-input"} "Title:"]
           [:input {:id "title-input"
                    :type "text"
                    :placeholder "Title for new rehearsal"
                    :name "title"
                    :require true
                    :value ""}]]
          [:button {:type "submit"} "Start new"]])

       [:h2 "Past ones"]

       [:ul
        (for [rehearsal closed-ones]
          [:li (rehearsal-link rehearsal)])]
       ]))})

(defn rehearsal-post! [{{{:keys [id]} :path
                         {:keys [title]} :form} :parameters
                        :keys [db whoami] :as req}]
  (rehearsal-service/insert-rehearsal! db whoami {:title title
                                                  :description ""
                                                  :start-time (Instant/now)
                                                  :duration nil})
  (rehearsal-index-page req))

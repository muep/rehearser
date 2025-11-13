(ns rehearser.ui.rehearsals
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.exercise :as exercise-service]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.service.variant :as variant-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.detail :as detail]
   [rehearser.ui.rehearsals.entry :as entry])
  (:import
   (java.time Duration Instant ZoneId)
   (java.time.format DateTimeFormatter)))

(def routes (concat detail/routes entry/routes))

(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
(def time-formatter (DateTimeFormatter/ofPattern "HH:mm"))
(def zone (ZoneId/of "UTC"))

(defn format-instant [instant]
  (.format formatter (.atZone instant zone)))

(defn format-time [instant]
  (.format time-formatter (.atZone instant zone)))

(defn rehearsal-link [{:keys [id title]} url-prefix]
  [:a {:href (str url-prefix "/rehearsals/" id "/rehearsal.html")} (hiccup/h title)])

(defn entry-add-page [{{{:keys [rehearsal-id]} :path} :parameters
                       :keys [db url-prefix whoami]}]
  (let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)
        tunes (exercise-service/find-all db whoami)
        variants (variant-service/find-all db whoami)]
    {:status 200
     :body
     (common-ui/page
      url-prefix whoami (str (:title rehearsal) " / New entry")
      [:main
       [:h1 (rehearsal-link rehearsal url-prefix) " / New entry"]

       [:form {:action (str url-prefix "/rehearsals/" rehearsal-id "/new-entry.html")
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
                   :keys [db url-prefix whoami] :as req}]
  (rehearsal-service/insert-entry! db whoami {:rehearsal-id rehearsal-id
                                              :exercise-id exercise-id
                                              :variant-id variant-id
                                              :entry-time (Instant/now)
                                              :remarks remarks})
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" rehearsal-id "/rehearsal.html")}})

(defn rehearsal-index-page [{:keys [db url-prefix whoami]}]
  {:status 200
   :body
   (let [[closed-ones open-ones] (->> (rehearsal-service/find-all db whoami)
                                      (group-by :is-open)
                                      sort
                                      (map second))]
     (common-ui/page
      url-prefix whoami "Rehearsals"
      [:main
       [:h1 "Rehearsals"]

       [:h2 "Current one"]

       (if-let [open-rehearsal (first open-ones)]
         [:span
          (rehearsal-link open-rehearsal url-prefix)
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
                    :required true
                    :value ""}]]
          [:button {:type "submit"} "Start new"]])

       [:h2 "Past ones"]

       [:ul
        (for [rehearsal closed-ones]
          [:li (rehearsal-link rehearsal url-prefix)])]
       ]))})

(defn rehearsal-post! [{{{:keys [id]} :path
                         {:keys [title]} :form} :parameters
                        :keys [db whoami] :as req}]
  (rehearsal-service/insert-rehearsal! db whoami {:title title
                                                  :description ""
                                                  :start-time (Instant/now)
                                                  :duration nil})
  (rehearsal-index-page req))

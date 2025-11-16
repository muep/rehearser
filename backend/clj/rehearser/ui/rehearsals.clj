(ns rehearser.ui.rehearsals
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.exercise :as exercise-service]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.service.variant :as variant-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.detail :as detail]
   [rehearser.ui.rehearsals.entry :as entry]
   [rehearser.ui.rehearsals.index :as index])
  (:import
   (java.time Duration Instant ZoneId)
   (java.time.format DateTimeFormatter)))

(def routes (concat detail/routes entry/routes index/routes))

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


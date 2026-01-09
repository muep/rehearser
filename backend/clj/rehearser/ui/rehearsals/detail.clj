(ns rehearser.ui.rehearsals.detail
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components])
  (:import (java.time Duration Instant ZoneId)))

(defn rehearsal-page [{{{:keys [id]} :path} :parameters
                       :keys [db url-prefix whoami] :as req}]
  (if-let [rehearsal (rehearsal-service/find-rehearsal db whoami id)]
    (let [{:keys [start-time duration title entries]} rehearsal
          now (Instant/now)
          seconds (if duration
                    duration
                    (.getSeconds (Duration/between start-time now)))]
      {:status 200
       :body
       (common-ui/page
        url-prefix whoami title
        [:main
         [:h1 [:a {:href (str url-prefix "/rehearsals.html")} "Rehearsals"]
          " / " (hiccup/h title)]

         [:p "On " (components/format-instant start-time)]
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
          (for [{:keys [exercise-title entry-time] :as entry} entries]
            [:li (components/entry-link entry url-prefix)
             " at " (hiccup/h (components/format-time entry-time))])]])})
    {:status 404
     :body "Did not find that rehearsal"}))

(defn rehearsal-close! [{{{:keys [id]} :path} :parameters
                          :keys [db url-prefix whoami] :as req}]
  (rehearsal-service/close-rehearsal! db whoami id (Instant/now))
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" id "/rehearsal.html")}})

(defn rehearsal-open! [{{{:keys [id]} :path} :parameters
                        :keys [db url-prefix whoami] :as req}]
  (rehearsal-service/update-rehearsal! db whoami id {:duration nil})
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
            :handler rehearsal-open!}}]])

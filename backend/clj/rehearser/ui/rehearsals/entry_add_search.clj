(ns rehearser.ui.rehearsals.entry-add-search
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.exercise :as exercise-service]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components])
  (:import (java.time.format DateTimeFormatter)))

(defn format-time [inst]
  (when inst
    ;; Convert Instant to LocalDateTime for proper formatting
    (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm")
             (.atZone inst (java.time.ZoneId/of "UTC")))))

(defn- new-entry-link [url-prefix rehearsal-id exercise]
  [:a {:href (str url-prefix "/rehearsals/" rehearsal-id "/new-entry.html?exercise-id=" (:id exercise))}
   (:title exercise)])

(defn entry-add-search-page [{{{:keys [rehearsal-id]} :path
                             {:keys [query]} :form} :parameters
                            :keys [db url-prefix whoami]}]
  (let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)
        recent-exercises (exercise-service/find-recent db whoami 5)
        frequent-exercises (exercise-service/find-frequent db whoami 5)
        search-results (when (and query (not (empty? query)))
                         (exercise-service/search db whoami query))]
    {:status 200
     :body (common-ui/page
            url-prefix whoami
            (str (:title rehearsal) " / Add entry")
            [:main
             [:h1 (components/rehearsal-link rehearsal url-prefix) " / Add entry"]

             [:form {:action (str url-prefix "/rehearsals/" rehearsal-id "/entry-add-search.html")
                     :method "post"}
              [:div
               [:label {:for "search-input"} "Search tunes:"]
               [:input {:type "text"
                        :id "search-input"
                        :name "query"
                        :value (or query "")
                        :autofocus true
                        :placeholder "Start typing tune name..."}]
               [:input {:type "submit" :value "Search"}]]]

             (when (and query (empty? search-results))
               [:div
                [:p {:class "search-no-results"} "No results found for " [:strong query]]])

             (when (seq search-results)
               [:div
                [:h3 (str "Search results for \"" query "\"")]
                [:ul
                 (for [exercise search-results]
                   [:li (new-entry-link url-prefix rehearsal-id exercise)])]])

             (when (seq recent-exercises)
               [:div
                [:h3 "Recent tunes"]
                [:ul
                 (for [exercise recent-exercises]
                   [:li
                    (new-entry-link url-prefix rehearsal-id exercise)
                    (when-let [latest-time (:latest-time exercise)]
                      [:span {:class "exercise-meta"}
                       " (Last: " (format-time latest-time) ")"])])]])

             (when (seq frequent-exercises)
               [:div
                [:h3 "Frequent tunes"]
                [:ul
                 (for [exercise frequent-exercises]
                   [:li (new-entry-link url-prefix rehearsal-id exercise)])]])])}))

(def routes
  [["/rehearsals/:rehearsal-id/entry-add-search.html"
    {:get {:parameters {:path {:rehearsal-id int?}}
           :handler entry-add-search-page}
     :post {:parameters {:path {:rehearsal-id int?}
                         :form {:query string?}}
            :handler entry-add-search-page}}]])

(ns rehearser.ui.rehearsals.entry-edit-search
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.exercise :as exercise-service]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components])
  (:import
   (java.net URLEncoder)
   (java.time.format DateTimeFormatter)))

(defn format-time [inst]
  (when inst
    (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm")
             (.atZone inst (java.time.ZoneId/of "UTC")))))

(defn- find-rehearsal-and-entry [db whoami rehearsal-id entry-id]
  (when-let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)]
    (when-let [entry (->> rehearsal :entries (some #(when (= entry-id (:id %)) %)))]
      [rehearsal entry])))

(defn- entry-link [url-prefix rehearsal-id entry-id exercise]
  [:a {:href (str url-prefix
                  "/rehearsals/"
                  rehearsal-id
                  "/entry/"
                  entry-id
                  "/entry.html?exercise-id="
                  (:id exercise))}
   (hiccup/h (:title exercise))])

(defn- url-encode [t]
  (URLEncoder/encode t "UTF-8"))

(defn entry-edit-search-page [{{{:keys [rehearsal-id id]} :path
                               {:keys [query]} :form} :parameters
                              :keys [db url-prefix whoami]}]
  (if-let [[rehearsal entry] (find-rehearsal-and-entry db whoami rehearsal-id id)]
    (let [recent-exercises (exercise-service/find-recent db whoami 5)
          frequent-exercises (exercise-service/find-frequent db whoami 5)
          search-results (when (and query (not (empty? query)))
                           (exercise-service/search db whoami query))
          search-action (str url-prefix
                             "/rehearsals/" rehearsal-id
                             "/entry/" id
                             "/entry-edit-search.html")
          link-to-entry (partial entry-link url-prefix rehearsal-id id)]
      {:status 200
       :body (common-ui/page
              url-prefix whoami
              (str (:title rehearsal) " / " (:exercise-title entry) " / Change tune")
              [:main
               [:h1 (components/rehearsal-link rehearsal url-prefix)
                " / " (components/entry-link entry url-prefix)
                " / Change tune"]

               [:p "Current tune: " [:strong (hiccup/h (:exercise-title entry))]]

               [:form {:action search-action
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

               (when (and (seq query) (empty? search-results))
                 [:div
                  [:p {:class "search-no-results"}
                   "No results found for " [:strong (hiccup/h query)]]
                  [:p "Create new tune: "
                   [:a {:href (str url-prefix
                                   "/tunes/new-tune.html?title="
                                   (url-encode query)
                                   "&redirect="
                                   (url-encode search-action))}
                    (str "Create \"" (hiccup/h query) "\"")]]])

               (when (seq search-results)
                 [:div
                  [:h3 (str "Search results for \"" (hiccup/h query) "\"")]
                  [:ul
                   (for [exercise search-results]
                     [:li (link-to-entry exercise)])]])

               (when (seq recent-exercises)
                 [:div
                  [:h3 "Recent tunes"]
                  [:ul
                   (for [exercise recent-exercises]
                     [:li
                      (link-to-entry exercise)
                      (when-let [latest-time (:latest-time exercise)]
                        [:span {:class "exercise-meta"}
                         " (Last: " (format-time latest-time) ") "])])]])

               (when (seq frequent-exercises)
                 [:div
                  [:h3 "Frequent tunes"]
                  [:ul
                   (for [exercise frequent-exercises]
                     [:li (link-to-entry exercise)])]])])})
    {:status 404
     :body (str "No entry " id " in rehearsal " rehearsal-id)}))

(def routes
  [["/rehearsals/:rehearsal-id/entry/:id/entry-edit-search.html"
    {:get {:parameters {:path {:rehearsal-id int?
                               :id int?}}
           :handler entry-edit-search-page}
     :post {:parameters {:path {:rehearsal-id int?
                                :id int?}
                         :form {:query string?}}
            :handler entry-edit-search-page}}]])

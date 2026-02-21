(ns rehearser.ui.tunes
  (:require
   [hiccup.core :as h]
   [rehearser.service.exercise :as exercise-service]

   [rehearser.ui.common :as common-ui]
   [rehearser.ui.header :refer [header]])
  (:import
   (java.time ZoneId)
   (java.time.format DateTimeFormatter)))

(def zone (ZoneId/of "UTC"))
(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
(defn format-instant [instant]
  (.format formatter (.atZone instant zone)))

(defn tune-link [{:keys [id title]} url-prefix]
  [:a {:href (str url-prefix "/tunes/" id "/tune.html")} (h/h title)])

;; Retained here for an idea that maybe we want a rehearsal link
;; instead of an entry link
#_(defn rehearsal-link [{:keys [rehearsal-id rehearsal-title entry-time]}
                      url-prefix]
  [:a {:href (str url-prefix "/rehearsals/" rehearsal-id "/rehearsal.html")}
   (h/h rehearsal-title)])

(defn entry-link [{:keys [id rehearsal-id rehearsal-title entry-time]}
                      url-prefix]
  [:a {:href (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/entry.html")}
   (h/h rehearsal-title)])

(defn tune-listing-page [{:keys [db url-prefix whoami]}]
  {:status 200
   :body (common-ui/page
          url-prefix whoami
          "Tunes"
          [:main
           [:h1 "Tunes"]
           [:a {:href (str url-prefix "/tunes/new-tune.html")} "Add new"]
           [:ul
            (for [exercise (exercise-service/find-all db whoami)]
              [:li (-> exercise (tune-link url-prefix))])]])})

(defn tune-details-page [{{{:keys [id]} :path} :parameters
                          :keys [db url-prefix whoami]}]
  (if-let [exercise (exercise-service/find-with-entries-by-id db whoami id)]
    (let [{:keys [title description entries]} exercise]
      {:status 200
       :body (common-ui/page
              url-prefix
              whoami
              (str "Tunes / "(h/h title))
              [:main
               [:h1 [:a {:href (str url-prefix "/tunes.html")} "Tunes"] " / " (h/h title)]

               [:h2 "Details"]
               [:form {:action (str url-prefix "/tunes/" id "/tune.html")
                       :method "post"}
                [:div
                 [:label {:for "title-input"} "Name:"]
                 [:input {:id "title-input"
                          :type "text"
                          :placeholder "Tune name"
                          :name "title"
                          :required true
                          :value title}]]
                [:div
                 [:label {:for "description-input"} "Description:"]
                 [:textarea {:id "description-input"
                             :type "text"
                             :placeholder "Description"
                             :name "description"
                             :required false} (h/h description)]]
                [:button {:type "submit"} "Save"]]
               [:h2 (str (count entries) " entries")]
               [:ul
                (for [{:as entry :keys [entry-time]} entries]
                  [:li (format-instant entry-time) " - " (entry-link entry url-prefix)])]])})
    {:status 404}))

(defn tune-details-post [{{{:keys [id]} :path
                           {:keys [title description]} :form} :parameters
                          :keys [db url-prefix whoami] :as req}]
  (exercise-service/update-by-id! db whoami id {:title title :description description})
  {:status 303
   :headers {"location" (str url-prefix "/tunes/" id "/tune.html")}})

(defn tune-add-page [{{{:keys [id]} :path} :parameters
                      :keys [db url-prefix whoami]}]
  {:status 200
   :body (common-ui/page
          url-prefix
          whoami
          (str "Tune "(h/h "Tunes / new"))
          [:main
           [:h1 [:a {:href (str url-prefix "/tunes.html")} "Tunes"] " / new"]

           [:form {:method "post"}
            [:div
             [:label {:for "title-input"} "Name:"]
             [:input {:id "title-input"
                      :type "text"
                      :placeholder "Tune name"
                      :name "title"
                      :required true}]]
            [:div
             [:label {:for "description-input"} "Description:"]
             [:textarea {:id "description-input"
                         :type "text"
                         :placeholder "Description"
                         :name "description"
                         :required false}]]
            [:button {:type "submit"} "Save"]]])})

(defn tune-post! [{{{:keys [title description]} :form} :parameters
                   :keys [db url-prefix whoami] :as req}]
  (exercise-service/add! db whoami {:title title :description description})
  {:status 303
   :headers {"location" (str url-prefix "/tunes.html")}})

(def routes
  [["/tunes.html"
    {:get {:handler tune-listing-page}}]
   ["/tunes/:id/tune.html"
    {:get {:parameters {:path {:id int?}}
           :handler tune-details-page}
     :post {:parameters {:path {:id int?}
                         :form {:title string?
                                :description string?}}
            :handler tune-details-post}}]
   ["/tunes/new-tune.html"
    {:get {:handler tune-add-page}
     :post {:parameters {:form {:title string?
                                :description string?}}
            :handler tune-post!}}]])

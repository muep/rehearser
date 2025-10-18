(ns rehearser.ui.tunes
  (:require
   [hiccup.core :as h]
   [rehearser.service.exercise :as exercise-service]

   [rehearser.ui.common :as common-ui]
   [rehearser.ui.header :refer [header]]))

(defn tune-link [{:keys [id title]}]
  [:a {:href (str "/tunes/" id "/tune.html")} (h/h title)])

(defn tune-listing-page [{:keys [db whoami]}]
  {:status 200
   :body (common-ui/page
          whoami
          "Tunes"
          [:main
           [:h1 "Tunes"]
           [:ul
            (for [exercise (exercise-service/find-all db whoami)]
              [:li (-> exercise tune-link)])]])})

(defn tune-details-page [{{{:keys [id]} :path} :parameters
                          :keys [db whoami]}]
  (if-let [exercise (-> (exercise-service/find-by-id db whoami id) first)]
    (let [{:keys [title description]} exercise]
      (println exercise)
      {:status 200
       :body (common-ui/page
              whoami
              (str "Tune "(h/h title))
              [:main
               [:h1 (h/h title)]

               [:form {:action (str "/tunes/" id "/tune.html")
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
                [:button {:type "submit"} "Save"]]])})
    {:status 404}))

(defn tune-details-post [{{{:keys [id]} :path
                           {:keys [title description]} :form} :parameters
                          :keys [db whoami] :as req}]
  (exercise-service/update-by-id! db whoami id {:title title :description description})
  (tune-details-page req))

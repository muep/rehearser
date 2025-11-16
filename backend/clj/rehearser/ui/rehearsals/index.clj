(ns rehearser.ui.rehearsals.index
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components])
  (:import
   (java.time Instant)))

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
          (components/rehearsal-link open-rehearsal url-prefix)
          " started at "
          (-> open-rehearsal
              :start-time
              components/format-instant
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
          [:li (components/rehearsal-link rehearsal url-prefix)])]]))})

(defn rehearsal-post! [{{{:keys [title]} :form} :parameters
                        :keys [db url-prefix whoami] :as req}]
  (rehearsal-service/insert-rehearsal! db whoami {:title title
                                                  :description ""
                                                  :start-time (Instant/now)
                                                  :duration nil})
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals.html")}})

(def routes
  [["/rehearsals.html"
    {:get {:handler rehearsal-index-page}
     :post {:parameters {:form {:title string?}}
            :handler rehearsal-post!}}]])

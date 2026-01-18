(ns rehearser.ui.rehearsals.entry
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.service.exercise :as exercise-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components]))

(defn entry-page [{{{:keys [rehearsal-id id]} :path} :parameters
                   :keys [db url-prefix whoami] :as req}]
  (if-let [[rehearsal entry]
           (let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)
                 entry (->> rehearsal :entries (some #(when (= id (:id %)) %)))]
             (when entry [rehearsal entry]))]
    (let [all-exercises (->> (exercise-service/find-all db whoami)
                             (sort-by :title #(compare (.toLowerCase ^String %1) (.toLowerCase ^String %2))))]
      {:status 200
       :body
       (common-ui/page
        url-prefix whoami (str (:title rehearsal) " / " (:exercise-title entry))
        [:main
         [:h1 (components/rehearsal-link rehearsal url-prefix) " / " (:exercise-title entry)]
         [:p "Practiced " (components/tune-link entry url-prefix) " at "
          (components/format-instant (:entry-time entry))
          " (or " [:a {:href (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/delete.html")} "didn't?"] ")"]
         [:form {:action (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/entry.html")
                 :method "post"}
          [:label {:for "exercise-id"} "Exercise:"]
          [:select {:id "exercise-id"
                    :name "exercise-id"}
           (for [exercise all-exercises]
             [:option {:value (:id exercise)
                       :selected (= (:id exercise) (:exercise-id entry))}
              (:title exercise)])]
          [:label {:for "remarks-input" :style "display: block; margin-bottom: 5px; font-weight: bold;"}
           "Notes:"]
          [:textarea {:id "remarks-input"
                      :name "remarks"
                      :placeholder "How did it go?"}
           (hiccup/h (:remarks entry))]
          [:input {:type "submit" :value "Save"}]]])})
    {:status 404
     :body (str "No entry " id " in rehearsal " rehearsal-id)}))

(defn entry-delete-page [{{{:keys [rehearsal-id id]} :path} :parameters
                         :keys [db url-prefix whoami] :as req}]
  (if-let [[rehearsal entry]
           (let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)
                 entry (->> rehearsal :entries (some #(when (= id (:id %)) %)))]
             (when entry [rehearsal entry]))]
    {:status 200
     :body
     (common-ui/page
      url-prefix whoami (str (:title rehearsal) " / " (:exercise-title entry) " / Delete entry")
      [:main
       [:h1
        (components/rehearsal-link rehearsal url-prefix) " / "
        (components/entry-link entry url-prefix) " / Delete entry"]
       [:p "Are you sure you want to delete the entry for "
        (:exercise-title entry) "?"]
       [:form {:action (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/delete.html")
               :method "post"}
        [:input {:type "submit" :value "Yes, delete this entry"}]]])}
    {:status 404
     :body (str "No entry " id " in rehearsal " rehearsal-id)}))

(defn entry-put! [{{{:keys [rehearsal-id id]} :path
                    {:keys [remarks exercise-id variant-id] :as entry} :form} :parameters
                   :keys [db url-prefix whoami] :as req}]
  (rehearsal-service/update-entry! db whoami id
                                   (merge (select-keys entry [:remarks :exercise-id :variant-id])
                                          {:id id}))
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/entry.html")}})

(defn entry-delete! [{{{:keys [rehearsal-id id]} :path} :parameters
                      :keys [db url-prefix whoami] :as req}]
  (rehearsal-service/delete-entry! db whoami id)
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" rehearsal-id "/rehearsal.html")}})

(def routes
  [["/rehearsals/:rehearsal-id/entry/:id/entry.html"
    {:get {:parameters {:path {:rehearsal-id int?
                               :id int?}}
           :handler entry-page}
     :post {:parameters {:path {:rehearsal-id int?
                                :id int?}
                         :form [:map
                                [:remarks {:optional true} string?]
                                [:variant-id {:optional true} int?]
                                [:exercise-id {:optional true} int?]]}
            :handler entry-put!}}]
   ["/rehearsals/:rehearsal-id/entry/:id/delete.html"
    {:get {:parameters {:path {:rehearsal-id int?
                              :id int?}}
           :handler entry-delete-page}
     :post {:parameters {:path {:rehearsal-id int?
                               :id int?}}
            :handler entry-delete!}}]])

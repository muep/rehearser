(ns rehearser.ui.rehearsals.entry
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.service.exercise :as exercise-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components]))

(defn entry-page [{{{:keys [rehearsal-id id]} :path} :parameters
                   :keys [db url-prefix whoami]}]
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
          " (or " [:a {:href (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/delete.html")} "didn't?"]
          " or " [:a {:href (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/duplicate.html")} "duplicate"] ")"]
         [:form {:action (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/entry.html")
                 :method "post"}
          [:div {:class "labeled-input"}
           [:label {:for "exercise-id"} "Exercise:"]
           [:select {:id "exercise-id"
                     :name "exercise-id"}
            (for [exercise all-exercises]
              [:option {:value (:id exercise)
                        :selected (= (:id exercise) (:exercise-id entry))}
               (:title exercise)])]]
          [:div {:class "labeled-input"}
           [:label {:for "remarks-input"} "Notes:"]
           [:textarea {:id "remarks-input"
                       :name "remarks"
                       :placeholder "How did it go?"}
            (hiccup/h (:remarks entry))]]
          [:input {:type "submit" :value "Save"}]]])})
    {:status 404
     :body (str "No entry " id " in rehearsal " rehearsal-id)}))

(defn entry-delete-page [{{{:keys [rehearsal-id id]} :path} :parameters
                         :keys [db url-prefix whoami]}]
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
                    {:as entry} :form} :parameters
                   :keys [db url-prefix whoami]}]
  (rehearsal-service/update-entry! db whoami id
                                   (merge (select-keys entry [:remarks :exercise-id :variant-id])
                                          {:id id}))
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/entry.html")}})

(defn entry-delete! [{{{:keys [rehearsal-id id]} :path} :parameters
                      :keys [db url-prefix whoami]}]
  (rehearsal-service/delete-entry! db whoami id)
  {:status 303
   :headers {"location" (str url-prefix "/rehearsals/" rehearsal-id "/rehearsal.html")}})

(defn entry-duplicate-page [{{{:keys [rehearsal-id id]} :path} :parameters
                             :keys [db url-prefix whoami]}]
  (if-let [entry (rehearsal-service/find-entry-by-id-with-title db whoami id)]
    (let [rehearsal (rehearsal-service/find-rehearsal db whoami (:rehearsal-id entry))]
      {:status 200
       :body
       (common-ui/page
        url-prefix whoami (str (:title rehearsal) " / " (:exercise-title entry) " / Duplicate entry")
        [:main
         [:h1
          (components/rehearsal-link rehearsal url-prefix) " / "
          (components/entry-link entry url-prefix) " / Duplicate entry"]
         [:p "Duplicate entry for " (hiccup/h (:exercise-title entry)) "?"]
         [:p "Exercise: " (hiccup/h (:exercise-title entry))]
         [:p "Variant: " (hiccup/h (:variant-title entry))]
         [:p "Remarks: " (hiccup/h (:remarks entry))]
         [:p "Entry time: " (components/format-instant (:entry-time entry))]
         [:form {:action (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/duplicate.html")
                 :method "post"}
          [:input {:type "submit" :value "Duplicate entry"}]
          [:a {:href (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/entry.html")} "Cancel"]]])})
    {:status 404
     :body (str "No entry " id " found")}))

(defn entry-duplicate! [{{{:keys [rehearsal-id id]} :path} :parameters
                        :keys [db url-prefix whoami]}]
  (if-let [new-entry (rehearsal-service/duplicate-entry! db whoami id)]
    {:status 303
     :headers {"location" (str url-prefix "/rehearsals/" rehearsal-id "/entry/" (:id new-entry) "/entry.html")}}
    {:status 404
     :body (str "No entry " id " found")}))

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
            :handler entry-delete!}}]
   ["/rehearsals/:rehearsal-id/entry/:id/duplicate.html"
    {:get {:parameters {:path {:rehearsal-id int?
                               :id int?}}
           :handler entry-duplicate-page}
     :post {:parameters {:path {:rehearsal-id int?
                                :id int?}}
            :handler entry-duplicate!}}]])

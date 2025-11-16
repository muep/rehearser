(ns rehearser.ui.rehearsals.entry
  (:require
   [hiccup.core :as hiccup]
   [rehearser.service.rehearsal :as rehearsal-service]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.rehearsals.components :as components]))

(defn entry-page [{{{:keys [rehearsal-id id]} :path} :parameters
                  :keys [db url-prefix whoami] :as req}]
  (if-let [[rehearsal entry]
           (let [rehearsal (rehearsal-service/find-rehearsal db whoami rehearsal-id)
                 entry (->> rehearsal :entries (some #(when (= id (:id %)) %)))]
             (when entry [rehearsal entry]))]
    {:status 200
     :body
     (common-ui/page
      url-prefix whoami (str (:title rehearsal) " / " (:exercise-title entry))
      [:main
       [:h1 (components/rehearsal-link rehearsal url-prefix) " / " (:exercise-title entry)]
       [:p "Practiced " (components/tune-link entry url-prefix) " at " (components/format-instant (:entry-time entry))]
       [:form {:action (str url-prefix "/rehearsals/" rehearsal-id "/entry/" id "/entry.html")
               :method "post"}
        [:textarea {:id "remarks-input"
                    :name "remarks"
                    :placeholder "How did it go?"}
         (hiccup/h (:remarks entry))]
        [:input {:type "submit" :value "Save"}]]])}
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
            :handler entry-put!}}]])

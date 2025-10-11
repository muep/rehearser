(ns rehearser.api.rehearsal
  (:require
   [rehearser.service.rehearsal :as service]
   [rehearser.malli :refer [Metadata Timestamp]]
   [malli.core :as m]
   [malli.util :as mu]))

(def RehearsalSave
  (m/schema
   [:map {:closed true}
    [:description string?]
    [:title (m/schema [:string {:min 1}])]
    [:start-time Timestamp]
    [:duration [:maybe integer?]]]))

(def Rehearsal
  (reduce mu/merge
          [Metadata
           RehearsalSave
           [:map {:closed true}
            [:is-open boolean?]]]))

(def Rehearsals (m/schema [:sequential Rehearsal]))

(def EntrySave
  (m/schema
   [:map {:closed true}
    [:exercise-id integer?]
    [:variant-id integer?]
    [:entry-time Timestamp]
    [:remarks string?]]))

(def Entry
  (mu/merge Metadata EntrySave))

(def Entries (m/schema [:sequential Entry]))

(defn post-rehearsal! [{{:keys [body]} :parameters
                        :keys [db whoami]}]
  {:status 200
   :body (service/insert-rehearsal! db whoami body)})

(defn get-rehearsals [{:keys [db whoami]}]
  {:status 200
   :body (service/find-all db whoami)})

(defn post-entry! [{{:keys [body]
                     {:keys [rehearsal-id]} :path} :parameters
                    :keys [db whoami]}]
  {:status 200
   :body (service/insert-entry! db whoami (-> body
                                              (assoc :rehearsal-id rehearsal-id)))})

(defn get-entries [{{{:keys [rehearsal-id]} :path} :parameters
                    :keys [db whoami]}]
  {:status 200
   :body (service/find-entries-of-rehearsal db whoami rehearsal-id)})

(defn- not-implemented! [_req]
  {:status 500
   :body "Not implemented"})

(def routes
  [["" {:get {:responses {200 {:body Rehearsals}}
              :handler get-rehearsals}
        :post {:parameters {:body RehearsalSave}
               :responses {200 {:body Rehearsal}}
               :handler post-rehearsal!}}]
   ["/:rehearsal-id" {:get {:responses {200 {:body Rehearsal}}
                            :handler not-implemented!}
                      :put {:parameters {:body RehearsalSave}
                            :responses {200 {:body Rehearsal}}
                            :handler not-implemented!}
                      :delete {:handler not-implemented!}}
    ["/entry" {:post {:parameters {:body EntrySave
                                   :path {:rehearsal-id int?}}
                      :responses {200 {:body Entry}}
                      :handler post-entry!}
               :get {:parameters {:path {:rehearsal-id int?}}
                     :responses {200 {:body Entries}}
                     :handler get-entries}}]]])

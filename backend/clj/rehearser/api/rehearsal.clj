(ns rehearser.api.rehearsal
  (:require [rehearser.response :as response]
            [rehearser.service.rehearsal :as service]
            [rehearser.malli :refer [Metadata save->update]]
            [malli.core :as m]
            [malli.util :as mu])
  (:import (java.time Instant)))

(defn update-if-present [m k f]
  (if (not (nil? (k m)))
    (update m k f)
    m))

(def RehearsalSave
  (m/schema [:map {:closed true}
             [:description string?]
             [:title (m/schema [:string {:min 1}])]
             [:start-time int?]
             [:duration [:maybe int?]]]))

(def RehearsalUpdate (save->update RehearsalSave))

(def EntrySave
  (m/schema [:map {:closed true}
             [:rehearsal-id int?]
             [:exercise-id int?]
             [:variant-id int?]
             [:entry-time int?]
             [:remarks string?]]))

(def EntryUpdate (save->update EntrySave))

(def Entry
  (-> Metadata
      (mu/merge EntrySave)))

(def Entries (m/schema [:sequential Entry]))

(def Rehearsal
  (-> Metadata
      (mu/merge RehearsalSave)
      (mu/merge [:map {:closed true}
                 [:is-open boolean?]
                 [:end-time [:maybe int?]]])))

(def Rehearsals (m/schema [:sequential Rehearsal]))

(def RehearsalDeep
  (-> Rehearsal
      (mu/merge (m/schema [:map {:closed true}
                           [:entries Entries]]))))

(defn api-rehearsal->service-rehearsal [r]
  (update r :start-time #(Instant/ofEpochSecond %)))

(defn service-rehearsal->api-rehearsal [r]
  (-> r
      (update-if-present :start-time #(.getEpochSecond %))
      (update-if-present :end-time #(.getEpochSecond %))))

(defn get-rehearsals [{:keys [db whoami]}]
  {:status 200
   :body (->> (service/find-all db whoami)
              (map service-rehearsal->api-rehearsal))})

(defn post-rehearsal! [{{:keys [body]} :parameters
                        :keys [db whoami]}]
  {:status 200
   :body (-> (service/add! db whoami (api-rehearsal->service-rehearsal body))
             service-rehearsal->api-rehearsal)})

(defn get-rehearsal [{:keys [db whoami]
                      {{:keys [rehearsal-id]} :path} :parameters}]
  {:status 200
   :body (service/find-by-id db whoami rehearsal-id)})

(defn delete-rehearsal! [{:keys [db whoami]
                          {{:keys [rehearsal-id]} :path} :parameters}]
  (response/modify-one
   (service/delete-by-id! db whoami rehearsal-id)))

(defn put-rehearsal! [{:keys [db whoami]
                       {{:keys [rehearsal-id]} :path
                        :keys [body]} :parameters}]
  (response/modify-one
   (service/update! db whoami rehearsal-id body)))

(defn get-entries [req]
  (throw (ex-info "not implemented" {})))

(defn post-entry! [req]
  (throw (ex-info "not implemented" {})))

(defn get-entry [req]
  (throw (ex-info "not implemented" {})))

(defn delete-entry! [req]
  (throw (ex-info "not implemented" {})))

(defn put-entry! [req]
  (throw (ex-info "not implemented" {})))

(def routes
  [[""
    {:get {:handler get-rehearsals
           :responses {200 {:body Rehearsals}}}
     :post {:handler post-rehearsal!
            :parameters {:body RehearsalSave}
            :responses {200 {:body Rehearsal}}}}]
   ["/:rehearsal-id"
    {:get {:handler get-rehearsal
           :parameters {:path {:rehearsal-id int?}}
           :responses {200 {:body RehearsalDeep}}}
     :put {:handler put-rehearsal!
           :parameters {:path {:rehearsal-id int?}
                        :body RehearsalUpdate}}
     :delete {:handler delete-rehearsal!
              :parameters {:path {:rehearsal-id int?}}}}]
   ["/:rehearsal-id/entry"
    {:get {:handler get-entries
           :parameters {:path {:rehearsal-id int?}}
           :responses {200 {:body Entries}}}
     :post {:handler post-entry!
            :parameters {:body EntrySave
                         :path {:rehearsal-id int?}}
            :responses {200 {:body Entry}}}}]
   ["/:rehearsal-id/entry/:entry-id"
    {:get {:handler get-entry
           :parameters {:path {:rehearsal-id int?
                               :entry-id int?}}
           :responses {200 {:body Entry}}}
     :put {:handler put-entry!
           :parameters {:path {:rehearsal-id int?
                               :entry-id int?}
                        :body EntryUpdate}
           :responses {200 {:body Entry}}}
     :delete {:handler delete-entry!
              :parameters {:path {:rehearsal-id int?
                               :entry-id int?}}}}]])
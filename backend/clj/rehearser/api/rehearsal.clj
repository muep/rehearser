(ns rehearser.api.rehearsal
  (:require [rehearser.service.rehearsal :as service]
            [rehearser.malli :refer [Metadata save->update]]
            [malli.core :as m]
            [malli.util :as mu]))

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
      (mu/merge RehearsalSave)))

(def Rehearsals (m/schema [:sequential Rehearsal]))

(def RehearsalDeep
  (-> Rehearsal
      (mu/merge (m/schema [:map {:closed true}
                           [:entries Entries]]))))


(defn get-rehearsals [req]
  (throw (ex-info "not implemented" {})))

(defn post-rehearsal! [req]
  (throw (ex-info "not implemented" {})))

(defn get-rehearsal [req]
  (throw (ex-info "not implemented" {})))

(defn delete-rehearsal! [req]
  (throw (ex-info "not implemented" {})))

(defn put-rehearsal! [req]
  (throw (ex-info "not implemented" {})))

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
           :responses {200 {:body Rehearsal}}}
     :put {:handler put-rehearsal!
           :parameters {:path {:rehearsal-id int?}
                        :body RehearsalUpdate}
           :responses {200 {:body Rehearsal}}}
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

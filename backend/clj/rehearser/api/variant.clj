(ns rehearser.api.variant
  (:require [rehearser.service.variant :as service]
            [rehearser.malli :refer [Metadata save->update]]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.error :as me]))

(def VariantSave
  (m/schema [:map {:closed true}
             [:description string?]
             [:title  (m/schema [:string {:min 1}])]]))

(def Variant
  (mu/merge Metadata VariantSave))

(def Variants (m/schema [:sequential Variant]))

(def VariantUpdate (save->update VariantSave))

(defn response-get-one [result]
  (if-let [body (first result)]
    {:status 200
     :body body}
    {:status 404
     :body "Resource was not found"}))

(defn response-modify-one [result]
  (if (<= 1 result)
    {:status 200
     :body (str result " items changed")}
    {:status 404
     :body "Not found, not changed"}))

(defn response-update-one [result]
  (if result
    {:status 200
     :body result}
    {:status 404
     :body "Not found, not changed"}))

(defn get-variants [{:keys [db whoami]}]
  {:status 200
   :body (service/find-all db whoami)})

(defn post-variant! [{{:keys [body]} :parameters
                       :keys [db whoami]}]
  {:body (service/add! db whoami body)
   :status 200})

(defn get-variant [{{{:keys [id]} :path} :parameters
                     :keys [db whoami parameters]}]
  (response-get-one (service/find-by-id db whoami id)))

(defn delete-variant! [{{{:keys [id]} :path} :parameters
                         :keys [db whoami]}]
  (response-modify-one (service/delete-by-id! db whoami id)))

(defn put-variant! [{{{:keys [id]} :path
                       {:keys [title description]} :body} :parameters
                      :keys [db whoami]}]
  (response-update-one
   (service/update-by-id! db whoami id {:title title
                                        :description description})))

(def routes
  [["" {:get {:handler get-variants
              :responses {200 {:body Variants}}}
        :post {:handler post-variant!
               :parameters {:body VariantSave}
               :responses {200 {:body Variant}}}}]
   ["/:id" {:get {:handler get-variant
                  :parameters {:path {:id int?}}
                  :responses {200 {:body Variant}}}
            :delete {:handler delete-variant!
                     :parameters {:path {:id int?}}}
            :put {:handler put-variant!
                  :parameters {:path {:id int?}
                               :body VariantUpdate}
                  :responses {200 {:body Variant}}}}]])

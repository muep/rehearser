(ns rehearser.api.exercise
  (:require [rehearser.service.exercise :as service]
            [schema.core :as s]))


(defn not-empty? [m] (not (empty? m)))

(defn optional-keys [s]
  (into {} (map (fn [[k v]] [(s/optional-key k) v]) s)))


(def ExerciseSave
  {:title (s/constrained s/Str not-empty?)
   :description s/Str})

(def Metadata
  {:id s/Int
   :account-id s/Int})

(def Exercise (merge Metadata ExerciseSave))

(def ExerciseUpdate
  (-> ExerciseSave
      optional-keys
      (s/constrained not-empty?)))


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

(defn get-exercises [{:keys [db whoami]}]
  {:status 200
   :body (service/find-all db whoami)})

(defn post-exercise! [{{:keys [body]} :parameters
                       :keys [db whoami]}]
  {:body (service/add! db whoami body)
   :status 200})

(defn get-exercise [{{{:keys [id]} :path} :parameters
                     :keys [db whoami parameters]}]
  (response-get-one (service/find-by-id db whoami id)))

(defn delete-exercise! [{{{:keys [id]} :path} :parameters
                         :keys [db whoami]}]
  (response-modify-one (service/delete-by-id! db whoami id)))

(defn put-exercise! [{{{:keys [id]} :path
                       {:keys [title description]} :body} :parameters
                      :keys [db whoami]}]
  (response-update-one
   (service/update-by-id! db whoami id {:title title
                                        :description description})))

(def routes
  [["" {:get {:handler get-exercises
              :responses {200 {:body [Exercise]}}}
        :post {:handler post-exercise!
               :parameters {:body ExerciseSave}
               :responses {200 {:body Exercise}}}}]
   ["/:id" {:get {:handler get-exercise
                  :parameters {:path {:id s/Int}}
                  :responses {200 {:body Exercise}}}
            :delete {:handler delete-exercise!
                     :parameters {:path {:id s/Int}}}
            :put {:handler put-exercise!
                  :parameters {:path {:id s/Int}
                               :body ExerciseUpdate}
                  :responses {200 {:body Exercise}}}}]])

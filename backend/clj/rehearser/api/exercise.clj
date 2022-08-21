(ns rehearser.api.exercise
  (:require [rehearser.service.exercise :as service]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.error :as me]))

(def Metadata
  (m/schema [:map {:closed true}
             [:id int?]
             [:account-id int?]]))

(def ExerciseSave
  (m/schema [:map {:closed true}
             [:description string?]
             [:title  (m/schema [:string {:min 1}])]]))

(def Exercise
  (mu/merge Metadata ExerciseSave))

(def Exercises (m/schema [:sequential Exercise]))

(def ExerciseUpdate
  (m/schema
   [:and
    (mu/optional-keys ExerciseSave)
    [:fn
     {:error/fn (constantly "At least one update is required")}
     (fn [m] (not (empty? m)))]]))

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
              :responses {200 {:body Exercises}}}
        :post {:handler post-exercise!
               :parameters {:body ExerciseSave}
               :responses {200 {:body Exercise}}}}]
   ["/:id" {:get {:handler get-exercise
                  :parameters {:path {:id int?}}
                  :responses {200 {:body Exercise}}}
            :delete {:handler delete-exercise!
                     :parameters {:path {:id int?}}}
            :put {:handler put-exercise!
                  :parameters {:path {:id int?}
                               :body ExerciseUpdate}
                  :responses {200 {:body Exercise}}}}]])

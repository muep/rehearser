(ns rehearser.api.exercise
  (:require [rehearser.service.exercise :as service]
            [clojure.spec.alpha :as s]))

(s/def ::id int?)
(s/def ::title string?)
(s/def ::description string?)
(s/def ::exercise-content
  (s/keys :req-un [::title
                   ::description]))
(s/def ::exercise
  (s/merge ::exercise-content
           (s/keys :req-un [::id])))

(s/def ::exercise-update
  (s/keys :opt-un [::title
                   ::description]))

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

(defn get-exercises [{:keys [db whoami]}]
  {:status 200
   :body (service/find-all db whoami)})

(defn post-exercise! [{:keys [db whoami body-params]
                      :as req}]
  {:body (service/add! db whoami body-params)
   :status 200})

(defn get-exercise [{{{:keys [id]} :path} :parameters
                     :keys [db whoami parameters]
                     :as req}]
  (response-get-one (service/find-by-id db whoami id)))

(defn delete-exercise! [{{:keys [id]} :path-params
                         :keys [db whoami]
                         :as req}]
  (response-modify-one (service/delete-by-id! db whoami (Integer/parseInt id))))


(defn put-exercise! [{{:keys [id]} :path-params
                      :keys [db whoami body-params]
                      :as req}]
  (response-modify-one
   (first
    (service/update-by-id! db whoami
                           (Integer/parseInt id)
                           (select-keys body-params [:title :description])))))

(def routes
  [["" {:get {:handler get-exercises}
        :post {:handler post-exercise!}}]
   ["/:id" {:get {:handler get-exercise
                  :parameters {:path {:id ::id}}
                  :responses {200 {:body ::exercise}}}
            :delete {:handler delete-exercise!
                     :parameters {:path {:id ::id}}}
            :put {:handler put-exercise!
                  :parameters {:path {:id ::id}
                               :body {:title ::title
                                      :description ::description}}}}]])

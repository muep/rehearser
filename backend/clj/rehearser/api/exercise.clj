(ns rehearser.api.exercise
  (:require [rehearser.service.exercise :as service]))


(defn not-implemented [what]
  (fn [req]
    (throw (ex-info (str what " not yet implemented") {}))))


(defn get-exercises [{:keys [db whoami]}]
  {:status 200
   :body (service/find-all db whoami)})

(defn post-exercise [{:keys [db whoami body-params]
                      :as req}]
  {:body (service/add! db whoami body-params)
   :status 200})

(def get-exercise (not-implemented "Getting a single exercise"))
(def put-exercise (not-implemented "Making changes to exercises"))

(def routes
  [["" {:get get-exercises
        :post post-exercise}]
   ["/:id" {:get get-exercise
            :put put-exercise}]])

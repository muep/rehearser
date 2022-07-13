(ns rehearser.api.exercise
  (:require [rehearser.service.exercise :as service]))


(defn not-implemented [what]
  (fn [req]
    (throw (ex-info (str what " not yet implemented") {}))))

(defn response-get-one [result]
  (if-let [body (first result)]
    {:status 200
     :body body}
    {:status 404
     :body "Resource was not found"}))

(defn get-exercises [{:keys [db whoami]}]
  {:status 200
   :body (service/find-all db whoami)})

(defn post-exercise [{:keys [db whoami body-params]
                      :as req}]
  {:body (service/add! db whoami body-params)
   :status 200})

(def get-exercise_ (not-implemented "Getting a single exercise"))

(defn get-exercise [{{:keys [id]} :path-params
                     :keys [db whoami]
                     :as req}]
  (response-get-one (service/find-by-id db whoami (Integer/parseInt id))))

(def put-exercise (not-implemented "Making changes to exercises"))

(def routes
  [["" {:get get-exercises
        :post post-exercise}]
   ["/:id" {:get get-exercise
            :put put-exercise}]])

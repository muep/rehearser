(ns rehearser.api.admin
  (:require [rehearser.db :refer [reset]]))

(defn reset-db [{:keys [db]}]
  (reset db)
  {:status 200})

(def routes [["/status" {:get (fn [_] {:status 200})}]
             ["/reset-db" {:post reset-db}]])

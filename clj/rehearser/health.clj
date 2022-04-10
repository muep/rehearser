(ns rehearser.health
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.sql SQLException)))

(defn- check-db [db]
  (try
    (jdbc/query db "select 1;")
    nil
    (catch SQLException e
      (str e))
    (catch IllegalArgumentException e
      (str e))))

(defn get-health [{:keys [db]}]
  (let [db-problem (check-db db)
        verdict (every? nil? [db-problem])]
    {:status (if verdict 200 500)
     :body
     {:verdict verdict
      :problems
      {:database db-problem}}}))

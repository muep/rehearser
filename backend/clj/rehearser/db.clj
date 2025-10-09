(ns rehearser.db
  (:require [clojure.java.io :as io]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc-sql]
            [clojure.string :as str])
  (:import
   (java.net ConnectException)
   (org.postgresql.util PSQLException)))

(defn schema-version [db]
  (try
    (-> (jdbc-sql/query db ["select max(version) as version from rehearser_schema;"])
        first
        :version)
    (catch PSQLException e
      (cond
        (= ConnectException (some-> e .getCause .getClass))
        (throw (ex-info "Failed to reach database for layout version check"
                        {:type :database-connection-problem}
                        (.getCause e)))
        (str/includes? (.getMessage e) "relation \"rehearser_schema\" does not exist")
        (throw (ex-info "Database does not contain a known database layout"
                        {:type :database-with-unknown-content}))
        :else (throw e)))))

(defn reset [db]
  (let [reset-stmts (slurp (io/resource "rehearser/rehearser-v1.sql"))]
    (jdbc/execute! db [reset-stmts])))

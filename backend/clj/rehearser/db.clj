(ns rehearser.db
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str])
  (:import
   (java.net ConnectException)
   (java.time Instant)
   (java.sql Timestamp PreparedStatement)
   (org.postgresql.util PSQLException)))

(extend-protocol jdbc/IResultSetReadColumn
  Timestamp
  (result-set-read-column [x _ _]
    (.toInstant x)))

(extend-protocol jdbc/ISQLParameter
  Instant
  (set-parameter [^Instant instant ^PreparedStatement stmt ^long i]
    (.setObject stmt i (Timestamp/from instant))))

(defn schema-version [db]
  (try
    (-> (jdbc/query db "select max(version) as version from rehearser_schema;")
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
    (jdbc/execute! db reset-stmts)))

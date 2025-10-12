(ns rehearser.db
  (:require [clojure.java.io :as io]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [next.jdbc :as jdbc]
            [next.jdbc.prepare :as jdbc-prep]
            [next.jdbc.result-set :as jdbc-rs]
            [next.jdbc.sql :as jdbc-sql]
            [clojure.string :as str])
  (:import
   (java.net ConnectException)
   (java.sql PreparedStatement Timestamp)
   (java.time Instant)
   (org.postgresql.util PSQLException)))

(extend-protocol jdbc-rs/ReadableColumn
  Timestamp
  (read-column-by-label [^Timestamp v _] (.toInstant v))
  (read-column-by-index [^Timestamp v _2 _3] (.toInstant v)))

(extend-protocol jdbc-prep/SettableParameter
  Instant
  (set-parameter [^Instant v ^PreparedStatement stmt ^long i]
    (.setTimestamp stmt i (Timestamp/from v))))

(defn def-db-fns [file]
  (hugsql/def-db-fns file {:adapter (next-adapter/hugsql-adapter-next-jdbc)}))

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

(ns rehearser.db
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc])
  (:import
   (java.time Instant)
   (java.sql Timestamp PreparedStatement)))

(extend-protocol jdbc/IResultSetReadColumn
  Timestamp
  (result-set-read-column [x _ _]
    (.toInstant x)))

(extend-protocol jdbc/ISQLParameter
  Instant
  (set-parameter [^Instant instant ^PreparedStatement stmt ^long i]
    (.setObject stmt i (Timestamp/from instant))))

(defn reset [db]
  (let [reset-stmts (slurp (io/resource "rehearser/rehearser-v1.sql"))]
    (jdbc/execute! db reset-stmts)))

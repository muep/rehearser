(ns rehearser.db
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]))

(defn reset [db]
  (let [reset-stmts (slurp (io/resource "rehearser/rehearser-v1.sql"))]
    (jdbc/execute! db reset-stmts)))

(ns rehearser.cmd.db-reset
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [rehearser.db :as db]))

(defn run [{{:keys [database-url]} :options :keys [subcmd-args]}]
  (let [reset-stmts (slurp (io/resource "rehearser/rehearser-v1.sql"))
        db {:connection-uri (db/libpq->jdbc database-url)}]
    (jdbc/execute! db reset-stmts)))

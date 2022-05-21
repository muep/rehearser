(ns rehearser.db
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]))

;; Note: no support for all options. Only those that have been needed
;; so far, and even these are processed in a pretty haphazard way.
(defn libpq->jdbc [uri]
  (let [[match username password hostname port dbname]
        (re-find #"postgres://(?<username>\w+):(?<password>\w+)@(?<host>[\w.-]+):(?<port>\w+)/(?<database>\w+)"
                 uri)]
    (assert (not (nil? match)))
    (str "jdbc:postgresql://"
         hostname ":" port "/" dbname
         "?user=" username "&password=" password)))

(defn db-url->db [database-url]
  {:connection-uri (libpq->jdbc database-url)})


(defn reset [db]
  (let [reset-stmts (slurp (io/resource "rehearser/rehearser-v1.sql"))]
    (jdbc/execute! db reset-stmts)))

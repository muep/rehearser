(ns rehearser.cmd.db-reset
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [rehearser.db :as db]))

(defn run [{{:keys [database-url]} :options :keys [subcmd-args]}]
  (db/reset {:connection-uri (db/libpq->jdbc database-url)}))

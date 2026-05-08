(ns rehearser.cmd.db-reset
  (:require [rehearser.db :as db]))

(defn run [{{:keys [jdbc-url]} :options}]
  (db/reset {:connection-uri jdbc-url}))

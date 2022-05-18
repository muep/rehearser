(ns rehearser.cmd.serve
  (:require
   [rehearser.cmd.common :refer [usage-error!]]
   [rehearser.db :refer [libpq->jdbc]]
   [rehearser.http-service :as http]))

(defn serve [{{:keys [database-url]} :options :keys [subcmd-args]}]
  (when-not (empty? subcmd-args)
    (usage-error! "usage: serve (no arguments supported)"))
  (http/run {:jdbc-url (libpq->jdbc database-url)}))

(ns rehearser.cmd.db-reset
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [rehearser.db :as db]))

(defn run [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (db/reset {:connection-uri jdbc-url}))

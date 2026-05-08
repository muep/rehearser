(ns rehearser.cmd.db-check
  (:require [rehearser.db :as db]))

(defn run [{{:keys [jdbc-url]} :options}]
  (println "Checking database at" jdbc-url)
  (try
    (let [db {:connection-uri jdbc-url}
          schema-ver (db/schema-version db)]
      (println "Detecting schema version" schema-ver))
    (catch Exception e
      (println e)
      (System/exit 1))))

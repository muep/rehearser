(ns rehearser.cmd.db-check
  (:require [rehearser.db :as db]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn  run [{{:keys [jdbc-url]} :options}]
  (println "Checking database at" jdbc-url)
  (try
    (let [db {:connection-uri jdbc-url}
          schema-ver (db/schema-version db)]
      (println "Detecting schema version" schema-ver))
    (catch Exception e
      (println e)
      (System/exit 1))))

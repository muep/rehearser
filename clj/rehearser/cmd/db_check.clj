(ns rehearser.cmd.db-check
  (:require [clojure.java.jdbc :as jdbc]
            [rehearser.db :as db]))

(defn run [{{:keys [database-url]} :options :keys [subcmd-args] }]
  (let [db {:connection-uri (db/libpq->jdbc database-url)}
        schema-ver (-> (jdbc/query db "select max(version) as version from rehearser_schema;")
                       first
                       :version)]
    (println "Detecting schema version" schema-ver)))

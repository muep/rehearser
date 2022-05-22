(ns rehearser.cmd.db-check
  (:require [clojure.java.jdbc :as jdbc]))

(defn run [{{:keys [jdbc-url]} :options :keys [subcmd-args] }]
  (println "Checking database at" jdbc-url)
  (let [db {:connection-uri jdbc-url}
        schema-ver (-> (jdbc/query db "select max(version) as version from rehearser_schema;")
                       first
                       :version)]
    (println "Detecting schema version" schema-ver)))

(ns rehearser.cmd.db-check
  (:require [clojure.java.jdbc :as jdbc])
  (:import (java.net ConnectException)
           (org.postgresql.util PSQLException)))

(defn run [{{:keys [jdbc-url]} :options :keys [subcmd-args] }]
  (println "Checking database at" jdbc-url)
  (try
    (let [db {:connection-uri jdbc-url}
          schema-ver (-> (jdbc/query db "select max(version) as version from rehearser_schema;")
                         first
                         :version)]
      (println "Detecting schema version" schema-ver))
    (catch PSQLException e
      (if (= ConnectException (-> e .getCause .getClass))
        (println "Failed to connect:" (-> e .getCause .getMessage))
        (println "PSQL error:" e))
      (System/exit 1))))

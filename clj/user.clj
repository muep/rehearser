(ns user
  (:require
   [clojure.tools.namespace.repl :refer [disable-unload! refresh]]
   [rehearser.db :refer [libpq->jdbc]]
   [rehearser.http-service :as service]
   [sysinfo :refer [sys-summary sys-stat]]))

(disable-unload!)

(defonce server (atom nil))

(defonce database-url "postgres://rehearser:rehearser@localhost:5432/rehearser")
(defonce port 8080)

(defn run []
  (refresh)
  (swap! server (fn [old-server]
                  (when old-server
                    (println "Shutting down" old-server)
                    (old-server))
                  (service/run {:jdbc-url (libpq->jdbc database-url)
                                :port port}))))

(defn stop []
  (swap! server (fn [old-server]
                  (when old-server
                    (println "Shutting down" old-server)
                    (old-server))
                  nil)))

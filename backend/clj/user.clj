(ns user
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [rehearser.db :as db]
   [rehearser.http-service :as service]
   [sysinfo :refer [sys-summary sys-stat]]
   [user.state :as state]))

(defn db []
  {:connection-uri (db/libpq->jdbc state/database-url)})

(defn run []
  (state/set-server! (service/run {:jdbc-url (db/libpq->jdbc state/database-url)
                                   :port state/port})))

(defn stop []
  (state/set-server! nil))

(defn restart []
  (stop)
  (refresh)
  (run))

(defn reset-db []
  (db/reset (db)))

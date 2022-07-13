(ns user
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [rehearser.db :as db]
   [rehearser.http-service :as service]
   [sysinfo :refer [sys-summary sys-stat]]
   [user.state :as state]))

(defn db []
  {:connection-uri state/jdbc-url})

(defn run []
  (state/set-server! (service/run {:jdbc-url state/jdbc-url
                                   :port state/port
                                   :static-file-dir "../front/public"})))

(defn stop []
  (state/set-server! nil))

(defn restart []
  (stop)
  (refresh)
  ((resolve 'user/run)))

(defn reset-db []
  (db/reset (db)))

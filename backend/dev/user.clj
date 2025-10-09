(ns user
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [rehearser.db :as db]
   [sysinfo :refer [sys-summary sys-stat]]
   [user.state :as state]))

(defn db []
  {:connection-uri state/jdbc-url})

(defn run []
  (state/set-server! nil)
  (state/set-server! ((resolve 'rehearser.http-service/run)
                      {:admin-pwhash state/admin-pwhash
                       :jdbc-url state/jdbc-url
                       :port state/port
                       :session-key state/session-key
                       :static-file-dir "front/public"})))

(defn stop []
  (state/set-server! nil))

(defn restart []
  (stop)
  (refresh)
  (run))

(defn reset-db []
  (db/reset (db)))

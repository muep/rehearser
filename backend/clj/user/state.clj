(ns user.state
  (:require
   [clojure.tools.namespace.repl :refer [disable-unload!]]))

(disable-unload!)

(defonce server (atom nil))
(defonce jdbc-url "jdbc:postgresql://localhost:5432/rehearser?user=rehearser&password=rehearser")
(defonce port 8080)

(defn set-server! [new-server]
  (swap! server (fn [old-server]
                  (when old-server
                    (println "Shutting down" old-server)
                    (old-server))
                  new-server)))

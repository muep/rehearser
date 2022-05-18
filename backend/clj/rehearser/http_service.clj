(ns rehearser.http-service
  (:require
   [clojure.tools.logging :as log]
   [crypto.random :as random]
   [reitit.ring :as reitit-ring]
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]
   [ring.middleware.session :as session-middleware]
   [ring.middleware.session.cookie :as cookie-session]
   [muuntaja.middleware :as muuntaja]
   [org.httpkit.server :as http-server]
   [hikari-cp.core :as hikari]
   [rehearser.api :as api]
   [rehearser.health :as health]))

(defn wrap-db [db]
  (fn [handler]
    (fn [req]
      (handler (assoc req :db db)))))

(defn wrap-session [key]
  (fn [handler]
    (session-middleware/wrap-session handler
                                     {:store (cookie-session/cookie-store {:key key})})))

(defn wrap-print-session [handler]
  (fn [{:keys [session] :as req}]
    (println "before:" session)
    (let [{:keys [session] :as resp} (handler req)]
      (println "after:" session)
      resp)))

(defn make-app [db session-key]
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/health" {:get health/get-health}]
     ["/api" api/routes]]
    {:data {:middleware [parameters-middleware
                         muuntaja/wrap-format-response
                         (wrap-db db)
                         (wrap-session session-key)
                         wrap-print-session
                         ]}})
   (reitit-ring/routes
    (reitit-ring/create-file-handler {:path "/"
                                      :root "web/"})
    (reitit-ring/create-default-handler))))

(defn run [{:keys [session-key jdbc-url port]
            :or {port 8080}}]
  (log/info "should listen on port" port "and use database at" jdbc-url)
  (let [ds (hikari/make-datasource {:jdbc-url jdbc-url})
        db {:datasource ds}
        key (or session-key (random/bytes 16))
        app (make-app db key)
        close-server (http-server/run-server app {:port port})]
    (fn []
      (close-server)
      (.close ds))))

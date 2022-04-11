(ns rehearser.http-service
  (:require
   [clojure.tools.logging :as log]
   [reitit.ring :as reitit-ring]
   [ring.middleware.session :as session-middleware]
   [ring.middleware.session.cookie :as cookie-session]
   [muuntaja.middleware :as muuntaja]
   [org.httpkit.server :as http-server]
   [hikari-cp.core :as hikari]
   [rehearser.health :as health]))

(defn wrap-db [db]
  (fn [handler]
    (fn [req]
      (handler (assoc req :db db)))))

(defn wrap-session [key]
  (fn [handler]
    (session-middleware/wrap-session handler
                                     {:store (cookie-session/cookie-store {:key key})})))

(defn make-app [db key]
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/health" {:get health/get-health}]]
    {:data {:middleware [(wrap-db db)
                         muuntaja/wrap-format-response
                         (wrap-session key)]}})
   (reitit-ring/routes
    (reitit-ring/create-file-handler {:path "/"
                                      :root "web/"})
    (reitit-ring/create-default-handler))))

(defn run [{:keys [jdbc-url port]
            :or {port 8080}}]
  (log/info "should listen on port" port "and use database at" jdbc-url)
  (let [ds (hikari/make-datasource {:jdbc-url jdbc-url})
        db {:datasource ds}
        app (make-app db nil)
        close-server (http-server/run-server app {:port port})]
    (fn []
      (close-server)
      (.close ds))))

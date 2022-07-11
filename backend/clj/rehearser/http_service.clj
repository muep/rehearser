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
  (fn [{:keys [remote-addr session] :as req}]
    (let [session-before session
          resp (handler req)
          session-after (:session resp)]
      (when (and (not (nil? session-after))
                 (not (= session-before session-after)))
        (log/info remote-addr session-before "->" session-after))
      resp)))

(defn session->whoami [{:keys [account-id account-name] :as session}]
  (when (and (int? account-id)
             (string? account-name)
             (not (empty? account-name)))
    {:account-id account-id
     :account-name account-name}))

(defn whoami-middleware [handler]
  (fn [{:keys [session] :as req}]
    (handler (assoc req :whoami (session->whoami session)))))

(defn make-app [db session-key static-file-dir]
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/health" {:get health/get-health}]
     ["/api" api/routes]]
    {:data {:middleware [parameters-middleware
                         muuntaja/wrap-format-response
                         (wrap-db db)
                         (wrap-session session-key)
                         whoami-middleware
                         wrap-print-session
                         ]}})
   (reitit-ring/routes
    (if (empty? static-file-dir)
      (reitit-ring/create-resource-handler {:path "/"})
      (reitit-ring/create-file-handler {:path "/"
                                        :root static-file-dir}))
    (reitit-ring/create-default-handler))))

(defn run [{:keys [session-key jdbc-url port static-file-dir]
            :or {port 8080}}]
  (log/info "should listen on port" port "and use database at" jdbc-url)

  (if (empty? static-file-dir)
    (log/info "Should serve static content from resources")
    (log/info "Should serve static content from" static-file-dir))

  (let [ds (hikari/make-datasource {:jdbc-url jdbc-url})
        db {:datasource ds}
        key (or session-key (random/bytes 16))
        app (make-app db key static-file-dir)
        close-server (http-server/run-server app {:port port})]
    (fn []
      (close-server)
      (.close ds))))

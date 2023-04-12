(ns rehearser.http-service
  (:require
   [clojure.tools.logging :as log]
   [crypto.random :as random]

   [reitit.ring :as reitit-ring]
   [reitit.ring.middleware.exception :as exception-middleware]
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]

   [reitit.coercion :refer [compile-request-coercers]]
   [reitit.coercion.malli :refer [coercion]]
   [reitit.ring.coercion :refer [coerce-request-middleware
                                 coerce-response-middleware]]

   [ring.middleware.session :as session-middleware]
   [ring.middleware.session.cookie :as cookie-session]
   [muuntaja.middleware :refer [wrap-format-negotiate
                                wrap-format-response
                                wrap-format-request]]
   [org.httpkit.server :as http-server]
   [hikari-cp.core :as hikari]
   [rehearser.api :as api]
   [rehearser.health :as health]
   [rehearser.reqstat :as reqstat]))

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

(defn wrap-disable-cache [handler]
  (fn [req]
    (-> (handler req)
        (assoc-in [:headers "Cache-Control"] "no-store"))))

(defn session->whoami [{:keys [account-id account-name account-admin?]}]
  (when (or account-admin?
            (and (int? account-id)
                 (string? account-name)
                 (not (empty? account-name))))
    {:account-id account-id
     :account-name account-name
     :account-admin? account-admin?}))

(defn whoami-middleware [handler]
  (fn [{:keys [session] :as req}]
    (handler (assoc req :whoami (session->whoami session)))))

(def api-adapter-middlewares
  [wrap-disable-cache
   wrap-format-negotiate
   wrap-format-response
   exception-middleware/exception-middleware
   wrap-format-request
   parameters-middleware
   coerce-request-middleware
   coerce-response-middleware])

(defn make-router [db session-key admin-pwhash]
  (let [reqstat (reqstat/reqstat-middleware+handler)]
    (reitit-ring/router
     [["/health" {:get health/get-health}]
      ["/api" (api/routes admin-pwhash (:get-handler reqstat))]]
     {:data {:coercion coercion
             :compile compile-request-coercers
             :middleware (-> (concat  [(:middleware reqstat)]
                                      api-adapter-middlewares
                                      [(wrap-db db)
                                       (wrap-session session-key)
                                       whoami-middleware
                                       wrap-print-session])
                             vec)}})))

(defn make-app [db session-key static-file-dir admin-pwhash]
  (reitit-ring/ring-handler
   (make-router db session-key admin-pwhash)
   (reitit-ring/routes
    (if (empty? static-file-dir)
      (do
        (log/info "Serving static content from resources")
        (reitit-ring/create-resource-handler {:path "/"
                                              :root "public"}))
      (do
        (log/info "Service static content from" static-file-dir)
        (reitit-ring/create-file-handler {:path "/"
                                          :root static-file-dir})))
    (reitit-ring/create-default-handler))))

(defn run [{:keys [admin-pwhash session-key jdbc-url port static-file-dir]
            :or {port 8080}}]
  (log/info "should listen on port" port "and use database at" jdbc-url)

  (if (empty? static-file-dir)
    (log/info "Should serve static content from resources")
    (log/info "Should serve static content from" static-file-dir))

  (let [ds (hikari/make-datasource {:jdbc-url jdbc-url})
        db {:datasource ds}
        key (or session-key (random/bytes 16))
        app (make-app db key static-file-dir admin-pwhash)
        close-server (http-server/run-server app {:port port})]
    (fn []
      (close-server)
      (.close ds))))

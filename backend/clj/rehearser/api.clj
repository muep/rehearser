(ns rehearser.api
  (:require [rehearser.api.account :as account]
            [rehearser.api.admin :as admin]
            [rehearser.api.exercise :as exercise]
            [rehearser.api.rehearsal :as rehearsal]
            [rehearser.api.sysinfo :as sysinfo]
            [rehearser.api.variant :as variant]
            [rehearser.exp :as exp]))

(defn wrap-require-account [handler]
  (fn [{:keys [whoami] :as req}]
    (if (and (some? whoami)
             (some? (:account-id whoami)))
      (handler req)
      {:status 303
       :headers {"Location" "../login.html"}
       :body "Logging in is required"})))

(defn wrap-require-admin [admin-pwhash]
  (if (empty? admin-pwhash)
    ;; The "normal" middleware checks for session attributes rather
    ;; than password hash. This check is for an extra safety
    ;; mechanism, where the normal middleware is replaced with an
    ;; always-rejecting one.
    (fn [handler]
      (fn [req]
        {:status 403}))
    (fn [handler]
      (fn [{{:keys [account-admin?]} :whoami
            :as req}]
        (if account-admin?
          (handler req)
          {:status 403})))))

(def api-metadata {:middleware [[wrap-require-account]]})

(defn routes [admin-pwhash get-reqstat]
  [["/login" {:post account/login}]
   ["/admin-login" {:post (account/admin-login admin-pwhash)}]
   ["/admin"
    {:middleware [[(wrap-require-admin admin-pwhash)]]}
    admin/routes]
   ["/signup" {:post account/signup}]
   ["/logout" {:post account/logout}]
   ["/whoami" {:get account/whoami}]
   ["/exercise" api-metadata exercise/routes]
   ["/rehearsal" api-metadata rehearsal/routes]
   ["/variant" api-metadata variant/routes]
   ["/params" {:get exp/params-get
               :post exp/params-post}]
   ["/fail" {:get exp/fail
             :post exp/fail}]
   ["/sys-stat" {:get sysinfo/sys-stat}]
   ["/sys-summary" {:get sysinfo/sys-summary}]
   ["/reqstat" {:get get-reqstat}]])

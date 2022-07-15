(ns rehearser.api
  (:require [rehearser.api.account :as account]
            [rehearser.api.admin :as admin]
            [rehearser.api.exercise :as exercise]
            [rehearser.api.sysinfo :as sysinfo]
            [rehearser.exp :as exp]))

(defn wrap-require-account [handler]
  (fn [{:keys [whoami] :as req}]
    (if (and (some? whoami)
             (some? (:account-id whoami)))
      (handler req)
      {:status 303
       :headers {"Location" "../login.html"}
       :body "Logging in is required"})))

(defn wrap-require-admin [handler]
  (fn [{{:keys [account-admin?]} :whoami
        :as req}]
    (if account-admin?
      (handler req)
      {:status 403})))

(def api-metadata {:middleware [[wrap-require-account]]})

(def routes [["/login" {:post account/login}]
             ["/admin-login" {:post account/admin-login}]
             ["/admin"
              {:middleware [[wrap-require-admin]]}
              admin/routes]
             ["/signup" {:post account/signup}]
             ["/logout" {:post account/logout}]
             ["/whoami" {:get account/whoami}]
             ["/exercise" api-metadata exercise/routes]
             ["/params" {:get exp/params-get
                         :post exp/params-post}]
             ["/fail" {:get exp/fail
                       :post exp/fail}]
             ["/sys-stat" {:get sysinfo/sys-stat}]
             ["/sys-summary" {:get sysinfo/sys-summary}]])

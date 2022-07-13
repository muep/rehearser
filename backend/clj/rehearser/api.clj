(ns rehearser.api
  (:require [rehearser.api.account :as account]
            [rehearser.api.exercise :as exercise]
            [rehearser.api.sysinfo :as sysinfo]
            [rehearser.exp :as exp]))

(defn wrap-require-account [handler]
  (fn [{:keys [whoami] :as req}]
    (if (some? whoami)
      (handler req)
      {:status 303
       :headers {"Location" "../login.html"}
       :body "Logging in is required"})))

(def api-metadata {:middleware [[wrap-require-account]]})

(def routes [["/login" {:post account/login}]
             ["/signup" {:post account/signup}]
             ["/"]
             ["/logout" {:post account/logout}]
             ["/whoami" {:get account/whoami}]
             ["/exercise" api-metadata exercise/routes]
             ["/params" {:get exp/params-get
                         :post exp/params-post}]
             ["/fail" {:get exp/fail
                       :post exp/fail}]
             ["/sys-stat" {:get sysinfo/sys-stat}]
             ["/sys-summary" {:get sysinfo/sys-summary}]])

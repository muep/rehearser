(ns rehearser.ui.index
  (:require
   [hiccup.core :as h]
   [rehearser.ui.account :as account-ui]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.header :refer [header]]))

(defn html [{{:keys [account-name] :as whoami} :whoami
             :keys [url-prefix]
             :as req}]
  {:status 200
   :body (str
          "<!DOCTYPE html>"
          (h/html
              [:html
               (common-ui/head url-prefix "index")
               [:body
                (if (nil? account-name)
                  (account-ui/login-form url-prefix)
                  (header url-prefix whoami))]]))})

(defn redirect-to-index [{:keys [url-prefix]}]
  {:status 302
   :headers {"Location" (str url-prefix "/index.html")}})

(def routes
  [[""
    {:get {:handler redirect-to-index
           :allow-anonymous? true}}]
   ["/"
    {:get {:handler redirect-to-index
           :allow-anonymous? true}}]
   ["/index.html"
    {:get {:handler html
           :allow-anonymous? true}}]])

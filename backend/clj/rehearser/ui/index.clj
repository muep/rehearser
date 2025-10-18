(ns rehearser.ui.index
  (:require
   [hiccup.core :as h]
   [rehearser.ui.account :as account-ui]
   [rehearser.ui.common :as common-ui]
   [rehearser.ui.header :refer [header]]))

(defn html [{{:keys [account-name] :as whoami} :whoami :as req}]
  {:status 200
   :body (str
          "<!DOCTYPE html>"
          (h/html
              [:html
               (common-ui/head "index")
               [:body
                (if (nil? account-name)
                  account-ui/login-form
                  (header whoami))]]))})

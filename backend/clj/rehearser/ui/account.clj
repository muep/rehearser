(ns rehearser.ui.account
  (:require [hiccup.core :refer [h]]))

(defn login-form [url-prefix]
  [:form {:action (str url-prefix "/api/login") :method "post"}
   [:input {:type "text" :placeholder "User name" :name "username" :id "username-input" :required true}]
   [:input {:type "password" :placeholder "Password" :name "password" :id "password-input" :required true}]
   [:button {:type "submit"} "Log in"]])

(defn logout-form [url-prefix account-name]
  [:form#logout-form {:action (str url-prefix "/api/logout") :method "post"}
   [:span (str "Logged in as " (h account-name))]
   [:button {:type "submit"} "Log out"]])

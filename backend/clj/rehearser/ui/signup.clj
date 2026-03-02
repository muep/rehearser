(ns rehearser.ui.signup
  (:require
   [hiccup.core :as h]
   [rehearser.ui.common :as common-ui]))

(defn signup-form [url-prefix]
  [:form {:action (str url-prefix "/api/signup") :method "post"}
   [:input {:type "text" :placeholder "User name" :name "username" :id "username-input" :required true}]
   [:input {:type "password" :placeholder "Password" :name "password" :id "password-input" :required true}]
   [:button {:type "submit"} "Sign up"]
   [:p "Already have an account? " [:a {:href (str url-prefix "/index.html")} "Log in"]]]) 

(defn html [{{:keys [account-name] :as whoami} :whoami
             :keys [url-prefix]
             :as req}]
  {:status 200
   :body (str
          "<!DOCTYPE html>"
          (h/html
              [:html
               (common-ui/head url-prefix "signup")
               [:body
                (if (nil? account-name)
                  (signup-form url-prefix)
                  [:div
                   [:p "You are already logged in."]
                   [:a {:href (str url-prefix "/index.html")} "Go to main page"]])]]))})

(def routes
  [["/signup.html"
    {:get {:handler html}}]])
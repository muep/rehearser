(ns rehearser.ui.common
  (:require
   [hiccup.core :as hiccup]
   [rehearser.ui.header :as header]))

(defn head [url-prefix title]
  [:head
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:meta {:charset "utf-8"}]
   [:title (if (empty? title) "Rehearser" (str "Rehearser - " title))]
   [:link {:rel "stylesheet" :href (str url-prefix "/styles.css")}]])

(defn page [url-prefix whoami title main]
  (str "<!DOCTYPE html>"
       (hiccup/html
           [:html
            (head url-prefix title)
            [:body
             (header/header url-prefix whoami)
             main]])))

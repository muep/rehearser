(ns rehearser.ui.header
  (:require
   [rehearser.ui.account :as account-ui]))

(defn header [url-prefix {:keys [account-name]}]
  [:header
   [:input#search {:type "text" :name "search"}]
   [:nav
    [:ul.nav-links
     [:li [:a {:href (str url-prefix "/index.html")} "Start"]]
     [:li [:a {:href (str url-prefix "/tunes.html")} "Tunes"]]
     [:li [:a {:href (str url-prefix "/rehearsals.html")} "Rehearsals"]]]]
   (account-ui/logout-form url-prefix account-name)])

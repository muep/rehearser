(ns rehearser.ui.header
  (:require
   [rehearser.ui.account :as account-ui]))

(defn header [{:keys [account-name]}]
  [:header
   [:input#search {:type "text" :name "search"}]
   [:nav
    [:ul.nav-links
     [:li [:a {:href "/index.html"} "Start"]]
     [:li [:a {:href "/tunes.html"} "Tunes"]]
     [:li [:a {:href "/rehearsals.html"} "Rehearsals"]]]]
   (account-ui/logout-form account-name)])

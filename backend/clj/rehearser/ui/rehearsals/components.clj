(ns rehearser.ui.rehearsals.components
  (:require [hiccup.core :as hiccup])
  (:import (java.time ZoneId)
           (java.time.format DateTimeFormatter)))

(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
(def time-formatter (DateTimeFormatter/ofPattern "HH:mm"))
(def zone (ZoneId/of "UTC"))

(defn rehearsal-link [{:keys [id title]} url-prefix]
  [:a {:href (str url-prefix "/rehearsals/" id "/rehearsal.html")}
   (hiccup/h title)])

(defn entry-link [{:keys [rehearsal-id id exercise-title]} url-prefix]
  [:a {:href (str url-prefix
                 "/rehearsals/"
                 rehearsal-id
                 "/entry/"
                 id
                 "/entry.html")}
   (hiccup/h exercise-title)])

(defn format-instant [instant]
  (.format formatter (.atZone instant zone)))

(defn format-time [instant]
  (.format time-formatter (.atZone instant zone)))

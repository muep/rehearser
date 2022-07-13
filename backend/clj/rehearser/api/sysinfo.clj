(ns rehearser.api.sysinfo
  (:require [sysinfo]))

(defn sys-stat [req]
  {:status 200
   :body (sysinfo/sys-stat)})

(defn sys-summary [req]
  {:status 200
   :body (sysinfo/sys-summary)})

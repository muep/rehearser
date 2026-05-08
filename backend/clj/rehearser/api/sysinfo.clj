(ns rehearser.api.sysinfo
  (:require [sysinfo]))

(defn sys-stat [_]
  {:status 200
   :body (sysinfo/sys-stat)})

(defn sys-summary [_]
  {:status 200
   :body (sysinfo/sys-summary)})

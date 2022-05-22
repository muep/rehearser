(ns rehearser.cmd.serve
  (:require
   [rehearser.cmd.common :refer [usage-error!]]
   [rehearser.http-service :as http]))

(defn serve [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (when-not (empty? subcmd-args)
    (usage-error! "usage: serve (no arguments supported)"))
  (http/run {:jdbc-url jdbc-url}))

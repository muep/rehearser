(ns rehearser.cmd.serve
  (:require
   [clojure.tools.cli :as cli]
   [rehearser.cmd.common :refer [usage-error!]]
   [rehearser.http-service :as http]))

(def serve-options
  [["-h" "--help" "Display help and exit"]
   [nil "--port PORT TCP port for serving HTTP"
    :default 8080
    :parse-fn (fn [o] (Integer/parseInt o))
    :validate [(fn [p] (< 0 p 65536)) "Must be a number from 1 to 65535"]]])


(defn serve [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (let [{{:keys [port]} :options
         :keys [arguments errors options summary]
         :as opts}
        (cli/parse-opts subcmd-args serve-options)]
    (when (not (empty? errors))
      (usage-error! "Bad arguments" {:summary summary
                                     :errors errors}))
    (http/run {:jdbc-url jdbc-url
               :port port})))

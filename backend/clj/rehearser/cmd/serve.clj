(ns rehearser.cmd.serve
  (:require
   [clojure.tools.cli :as cli]
   [rehearser.cmd.common :refer [usage-error!]]
   [rehearser.http-service :as http]))

(def serve-options
  [["-h" "--help" "Display help and exit"]
   [nil "--port PORT TCP port for serving HTTP"]])


(defn serve [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (let [{:keys [arguments errors options summary] :as opts}
        (cli/parse-opts subcmd-args serve-options)]
    (when (not (empty? errors))
      (throw (ex-info "Bad arguments" {:summary summary
                                       :errors errors
                                       :type :usage})))
    (let [{:keys [port]
           :or {port "8080"}} options]
      (http/run {:jdbc-url jdbc-url
                 :port (Integer/parseInt port)}))))

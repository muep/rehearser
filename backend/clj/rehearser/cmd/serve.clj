(ns rehearser.cmd.serve
  (:require
   [clojure.java.io :as io]
   [clojure.tools.cli :as cli]
   [rehearser.cmd.common :refer [check-parse-result!]]
   [rehearser.hex :refer [hex->bytes]]
   [rehearser.http-service :as http]))

(def serve-options
  [["-h" "--help" "Display help and exit"]
   [nil "--port PORT" "Select TCP port for serving HTTP"
    :default 8080
    :parse-fn (fn [o] (Integer/parseInt o))
    :validate [(fn [p] (< 0 p 65536)) "Must be a number from 1 to 65535"]]
   [nil "--static-file-dir PATH" "serve static files from PATH"
    :validate [(fn [p] (-> (io/file p)
                           .isDirectory))
               "Requested static file directory does not exist"]]])

(defn env->session-key []
  (let [k (-> "SESSION_KEY"
              System/getenv
              hex->bytes)]
    (if (= 16 (count k)) k nil)))

(defn serve [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (let [{{:keys [port static-file-dir]} :options
         :keys [arguments errors options summary]
         :as opts}
        (cli/parse-opts subcmd-args serve-options)
        session-key (env->session-key)]
    (check-parse-result! opts)
    (http/run {:jdbc-url jdbc-url
               :port port
               :session-key session-key
               :static-file-dir static-file-dir})))

(ns rehearser.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [rehearser.cmd.common :as common-cmd]
            [rehearser.db-url :refer [libpq->jdbc]])
  (:gen-class))

(def toplevel-options
  [["-h" "--help" "Display help and exit"]
   [nil "--database DATABASE Database URL"]])

(defn account-add [opts]
  (require 'rehearser.cmd.account)
  ((resolve 'rehearser.cmd.account/add) opts))

(defn account-list [opts]
  (require 'rehearser.cmd.account)
  ((resolve 'rehearser.cmd.account/-list) opts))

(defn account-passwd [opts]
  (require 'rehearser.cmd.account)
  ((resolve 'rehearser.cmd.account/passwd) opts))

(defn db-check [opts]
  (require 'rehearser.cmd.db-check)
  ((resolve 'rehearser.cmd.db-check/run) opts))

(defn db-reset [opts]
  (require 'rehearser.cmd.db-reset)
  ((resolve 'rehearser.cmd.db-reset/run) opts))

(defn pw-check [opts]
  (require 'rehearser.cmd.pw)
  ((resolve 'rehearser.cmd.pw/check) opts))

(defn pw-hash [opts]
  (require 'rehearser.cmd.pw)
  ((resolve 'rehearser.cmd.pw/hashpw) opts))

(defn serve [opts]
  (require 'rehearser.cmd.serve)
  ((resolve 'rehearser.cmd.serve/serve) opts))

(def subcommands
  {:account-add [account-add "Add an account"]
   :account-list [account-list "List accounts"]
   :account-passwd [account-passwd "Reset password of account"]
   :db-check [db-check "Check database connectivity"]
   :db-reset [db-reset "Reset database contents"]
   :pw-check [pw-check "Check a password hash"]
   :pw-hash [pw-hash "Produce a password hash"]
   :serve [serve "Run the http service"]})


(defn subcmd-description []
  (str/join \newline
            (concat
             ["subcommands:"]
             (map (fn [[nom [_ desc]]]
                              (str "    " (name nom) ": " desc))
                            subcommands))))

(defn env->jdbc-url []
  (let [database-url (System/getenv "DATABASE_URL")
        jdbc-url (System/getenv "JDBC_URL")]
    (cond
      (not (nil? jdbc-url))
      {:jdbc-url jdbc-url}

      (not (nil? database-url))
      {:jdbc-url (libpq->jdbc database-url)}

      :else
      {:jdbc-url "jdbc:postgresql://localhost:5432/rehearser?user=rehearser&password=rehearser"})))

(def env->options env->jdbc-url)

(defn parse-args [args]
  (let [{:keys [arguments errors options summary] :as opts}
        (cli/parse-opts args toplevel-options :in-order true)
        [subcmd-name & subcmd-args] arguments]
    (common-cmd/check-parse-result! ["rehearser"] opts
                                    [(subcmd-description)])

    (when (nil? subcmd-name)
      (common-cmd/usage-error! "Subcommand is required"
                               (common-cmd/usage-msg ["rehearser"]
                                                     summary
                                                     [(subcmd-description)])))

    (if-let [subcmd (-> subcommands  (get (keyword subcmd-name)) first)]
      {:options options
       :subcmd subcmd
       :subcmd-args subcmd-args
       :subcmd-name subcmd-name}
      (common-cmd/usage-error! (str "Unexpected subcommand " subcmd-name)
                               (common-cmd/usage-msg ["rehearser"]
                                                     summary
                                                     [(subcmd-description)])))))

(defn -main [& args]
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))
  (try
    ;; Only the top-level arguments are parsed here. Further parsing
    ;; will be done when the subcommand has been selected and its
    ;; namespace has been loaded as well.
    (let [{:keys [help status options subcmd subcmd-args subcmd-name]} (parse-args args)]
      (subcmd {:options (merge (env->options) options)
               :subcmd-args subcmd-args}))
    (catch clojure.lang.ExceptionInfo e
          (case (-> e ex-data :type)
            :usage (do
                     (println (.getMessage e))
                     (println (-> e ex-data :usage))
                     (System/exit 2))
            :help (println (-> e ex-data :usage))
            (throw e)))))

(ns rehearser.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
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

(defn serve [opts]
  (require 'rehearser.cmd.serve)
  ((resolve 'rehearser.cmd.serve/serve) opts))

(def subcommands
  {:account-add [account-add "Add an account"]
   :account-list [account-list "List accounts"]
   :account-passwd [account-passwd "Reset password of account"]
   :db-check [db-check "Check database connectivity"]
   :db-reset [db-reset "Reset database contents"]
   :serve [serve "Run the http service"]})

(defn usage [summary]
  (str/join \newline
            ["Usage:"
             "    rehearser [options]"
             "    rehearser <subcommand> --help"
             "    rehearser [options]  <subcommand> [subcommand-options]"
             "Options:"
             summary
             "Subcommands:"
             (str/join \newline
                       (map (fn [[nom [_ desc]]]
                              (str "    " (name nom) ": " desc))
                            subcommands))]))

(defn env->jdbc-url []
  (let [database-url (System/getenv "DATABASE_URL")
        jdbc-url (System/getenv "JDBC_URL")]
    (cond
      (not (nil? jdbc-url))
      {:jdbc-url jdbc-url}

      (not (nil? database-url))
      {:jdbc-url (libpq->jdbc database-url)}

      :else
      {})))

(def env->options env->jdbc-url)

(defn parse-args [args]
  (let [{:keys [arguments errors options summary] :as opts}
        (cli/parse-opts args toplevel-options :in-order true)
        [subcmd-name & subcmd-args] arguments]
    (cond
      (:help options)
      {:exit-with-message (usage summary)
       :status 0}

      errors
      {:exit-with-message (str/join \newline errors)
       :status 1}

      (nil? subcmd-name)
      {:exit-with-message (str/join \newline
                                    ["Expected a subcommand" (usage summary)])
       :status 1}

      :else
      (if-let [subcmd (-> subcommands  (get (keyword subcmd-name)) first)]
        {:options options
         :subcmd subcmd
         :subcmd-args subcmd-args
         :subcmd-name subcmd-name}
        {:exit-with-message (str/join \newline
                                      [(str "Unexpected subcommand " subcmd-name)
                                       (usage summary)])
         :status 1}))))



(defn subcmd-usage-msg [subcmd-name errors summary]
  (str
   (if (empty? errors) "" (str (first errors) "\n"))
   (str/join "\n" [(str "usage: rehearser " subcmd-name " [options]")
                   "options:"
                   summary])))

(defn -main [& args]
  (let [{:keys [exit-with-message status options subcmd subcmd-args subcmd-name]} (parse-args args)]
    (when exit-with-message
      (println exit-with-message)
      (System/exit status))
    (try
      (subcmd {:options (merge (env->options) options)
               :subcmd-args subcmd-args})
      (catch clojure.lang.ExceptionInfo e
        (case (-> e ex-data :type)
          :usage (do
                   (println (subcmd-usage-msg subcmd-name
                                              (-> e ex-data :errors)
                                              (-> e ex-data :summary)))
                   (System/exit 2))
          :help (println (subcmd-usage-msg subcmd-name
                                           []
                                           (-> e ex-data :summary)))
          (throw e))))))

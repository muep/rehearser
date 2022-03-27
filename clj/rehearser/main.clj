(ns rehearser.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]))

(def toplevel-options
  [["-h" "--help" "Display help and exit"]
   [nil "--database DATABASE Database URL"]])

(defn db-check [opts]
  (require 'rehearser.cmd.db-check)
  ((resolve 'rehearser.cmd.db-check/run) opts))

(def subcommands
  {:db-check [db-check "Check database connectivity"]})

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

(defn env->database-url []
  (if-let [db-url (System/getenv "DATABASE_URL")]
    {:database-url db-url}
    {}))

(def env->options env->database-url)

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
         :subcmd-args subcmd-args}
        {:exit-with-message (str/join \newline
                                      [(str "Unexpected subcommand " subcmd-name)
                                       (usage summary)])
         :status 1}))))

(defn -main [& args]
  (let [{:keys [exit-with-message status options subcmd subcmd-args]} (parse-args args)]
    (when exit-with-message
      (println exit-with-message)
      (System/exit status))
    (subcmd {:options (merge (env->options) options)
             :subcmd-args subcmd-args})))

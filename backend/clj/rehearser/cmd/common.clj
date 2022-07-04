(ns rehearser.cmd.common
  (:require [clojure.string :as str]))

(defn usage-msg [cmd-path summary extra-messages]
  (str/join "\n"
            (concat
             [(str"usage: " (str/join " " cmd-path) " [options]")
              "options:"
              summary]
             extra-messages)))

(defn usage-error! [message usage-text]
  (throw (ex-info message {:type :usage
                           :usage usage-text})))

(defn help! [usage-text]
  (throw (ex-info "Help printout requested" {:type :help
                                             :usage usage-text})))

(defn check-parse-result! [cmd-path {:keys [errors summary] {:keys [help]}:options} extra-messages]
  (when help
    (help! (usage-msg cmd-path summary extra-messages)))
  (when (not (empty? errors))
    (usage-error! (first errors)
                  (usage-msg cmd-path summary extra-messages))))

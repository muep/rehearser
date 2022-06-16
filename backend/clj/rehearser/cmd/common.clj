(ns rehearser.cmd.common)

(defn usage-error! [message data]
  (throw (ex-info message (assoc data :type :usage))))

(defn help! [data]
  (throw (ex-info "Help printout requested" (assoc data :type :help))))

(defn check-parse-result! [{:keys [errors summary] {:keys [help]}:options}]
  (when help
    (help! {:errors errors
            :summary summary}))
  (when (not (empty? errors))
    (usage-error! "Bad command line arguments"
                  {:errors errors
                   :summary summary})))


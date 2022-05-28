(ns rehearser.cmd.common)

(defn usage-error! [message data]
  (throw (ex-info message (assoc data :type :usage))))

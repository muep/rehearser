(ns rehearser.cmd.common)

(defn usage-error! [message args]
  (throw (ex-info message {:type :usage
                           :args args})))

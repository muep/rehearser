(ns rehearser.db)

;; Note: no support for all options. Only those that have been needed
;; so far, and even these are processed in a pretty haphazard way.
(defn libpq->jdbc [uri]
  (let [[match username password hostname port dbname]
        (re-find #"postgres://(?<username>\w+):(?<password>\w+)@(?<host>[\w.-]+):(?<port>\w+)/(?<database>\w+)"
                 uri)]
    (assert (not (nil? match)))
    (str "jdbc:postgresql://"
         hostname ":" port "/" dbname
         "?user=" username "&password=" password)))

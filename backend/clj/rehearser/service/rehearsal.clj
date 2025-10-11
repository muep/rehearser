(ns rehearser.service.rehearsal
  (:require [rehearser.db :refer [def-db-fns]]))

(def-db-fns "rehearser/rehearsal.sql")

(defn start! [db whoami rehearsal]
  (rehearsal-start! db (merge (select-keys whoami [:account-id])
                              (select-keys rehearsal [:title :description]))))

(defn end! [db whoami]
  (rehearsal-end! db whoami))

(defn find-all [db whoami]
  (rehearsal-select db whoami))

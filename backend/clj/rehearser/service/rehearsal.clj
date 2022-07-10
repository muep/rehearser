(ns rehearser.service.rehearsal
  (:require [jeesql.core :refer [defqueries]]))

(defqueries "rehearser/rehearsal.sql")

(defn start! [db whoami rehearsal]
  (rehearsal-start<! db (merge (select-keys whoami [:account-id])
                               (select-keys rehearsal [:title :description]))))

(defn end! [db whoami]
  (rehearsal-end! db whoami))

(defn find-all [db whoami]
  (rehearsal-select db whoami))

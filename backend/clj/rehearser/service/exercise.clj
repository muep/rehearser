(ns rehearser.service.exercise
  (:require
   [clojure.java.jdbc :as jdbc]
   [jeesql.core :refer [defqueries]]))

(defqueries "rehearser/exercise.sql")

(defn add! [db whoami exercise]
  (exercise-insert<! db (merge (select-keys whoami [:account-id])
                               (select-keys exercise [:title :description]))))

(defn find-by-id [db whoami id]
  (exercise-by-id db {:id id
                      :account-id (:account-id whoami)}))

(defn find-all [db whoami]
  (exercise-select-all db (select-keys whoami [:account-id])))

(defn update-by-id! [db whoami id exercise]
  (jdbc/update! db :exercise exercise ["\"account-id\" = ? and id = ?" (:account-id whoami) id]))

(defn delete-by-id! [db whoami id]
  (exercise-delete! db {:id id
                        :account-id (:account-id whoami)}))

(ns rehearser.service.variant
  (:require
   [jeesql.core :refer [defqueries]]
   [rehearser.misc :refer [select-or-nil-keys]]))

(defqueries "rehearser/variant.sql")

(defn add! [db whoami variant]
  (variant-insert<! db (merge (select-keys whoami [:account-id])
                              (select-keys variant [:title :description]))))

(defn find-by-id [db whoami id]
  (variant-by-id db {:id id
                      :account-id (:account-id whoami)}))

(defn find-all [db whoami]
  (variant-select-all db (select-keys whoami [:account-id])))

(defn update-by-id! [db whoami id variant]
  (variant-update<! db (merge
                        {:id id}
                        (select-keys whoami [:account-id])
                        (select-or-nil-keys variant [:title :description]))))

(defn delete-by-id! [db whoami id]
  (variant-delete! db {:id id
                       :account-id (:account-id whoami)}))

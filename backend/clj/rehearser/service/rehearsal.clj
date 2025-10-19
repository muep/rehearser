(ns rehearser.service.rehearsal
  (:require
   [next.jdbc :as jdbc]
   [rehearser.db :refer [def-db-fns]]))

(def-db-fns "rehearser/rehearsal.sql")

(defn insert-rehearsal! [db whoami rehearsal]
  (rehearsal-insert! db (merge (select-keys whoami [:account-id])
                               (select-keys rehearsal [:title
                                                       :description
                                                       :start-time
                                                       :duration]))))

(defn update-rehearsal! [db whoami id rehearsal]
  (rehearsal-update! db (merge {:id id}
                               (select-keys whoami [:account-id])
                               (select-keys rehearsal [:title
                                                       :description
                                                       :start-time
                                                       :duration]))))

(defn close-rehearsal! [db whoami id end-time]
  (rehearsal-close! db {:id id
                        :account-id (:account-id whoami)
                        :end-time end-time}))

(defn find-all [db whoami]
  (rehearsal-select db whoami))

(defn insert-entry! [db whoami entry]
  (entry-insert! db (merge (select-keys whoami [:account-id])
                           (select-keys entry [:rehearsal-id
                                               :exercise-id
                                               :variant-id
                                               :entry-time
                                               :remarks]))))

(defn update-entry! [db whoami id entry]
  (entry-update! db (merge (select-keys entry [:entry-time :remarks :variant-id :exercise-id])
                           {:id id}
                           (select-keys whoami [:account-id]))))

(defn find-entries-of-rehearsal [db {:keys [account-id]} rehearsal-id]
  (entry-select db {:account-id account-id
                    :rehearsal-id rehearsal-id}))

(defn find-rehearsal [db {:keys [account-id]} rehearsal-id]
  (jdbc/with-transaction [tx db]
    (when-let [rehearsal (rehearsal-select-by-id tx {:account-id account-id
                                           :rehearsal-id rehearsal-id})]
      (assoc rehearsal :entries (entry-select-with-title tx {:account-id account-id
                                                             :rehearsal-id rehearsal-id})))))

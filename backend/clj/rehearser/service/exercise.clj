(ns rehearser.service.exercise
  (:require
   [next.jdbc :refer [with-transaction]]
   [rehearser.db :refer [def-db-fns]]
   [rehearser.misc :refer [select-or-nil-keys]]))

(def-db-fns "rehearser/exercise.sql")

(defn add! [db whoami exercise]
  (exercise-insert! db (merge (select-keys whoami [:account-id])
                              (select-keys exercise [:title :description]))))

(defn find-by-id [db whoami id]
  (exercise-by-id db {:id id
                      :account-id (:account-id whoami)}))

(defn find-with-entries-by-id [db whoami id]
  (with-transaction [tx db]
    (when-let [exercise (first (find-by-id tx whoami id))]
      (assoc exercise :entries (entries-by-exercise-id
                                tx {:id id
                                    :account-id (:account-id whoami)})))))

(defn find-all [db whoami]
  (exercise-select-all db (select-keys whoami [:account-id])))

(defn update-by-id! [db whoami id exercise]
  (exercise-update! db (merge
                        {:id id}
                        (select-keys whoami [:account-id])
                        (select-or-nil-keys exercise [:title :description]))))

(defn delete-by-id! [db whoami id]
  (exercise-delete! db {:id id
                        :account-id (:account-id whoami)}))

(defn search [db whoami query]
  (search-exercises db (merge (select-keys whoami [:account-id])
                             {:query (str "%" query "%")})))

(defn find-recent [db whoami limit]
  (find-recent-exercises db (merge (select-keys whoami [:account-id])
                                   {:limit limit})))

(defn find-frequent [db whoami limit]
  (find-frequent-exercises db (merge (select-keys whoami [:account-id])
                                     {:limit limit})))

(ns rehearser.service.rehearsal
  (:require [jeesql.core :refer [defqueries]]))

(defqueries "rehearser/rehearsal.sql")

(defn start! [db whoami rehearsal]
  (rehearsal-start<! db (merge (select-keys whoami [:account-id])
                               (select-keys rehearsal [:title :description]))))

(defn end! [db whoami]
  (rehearsal-end<! db whoami))

(defn delete-by-id! [db whoami rehearsal-id]
  (let [params (merge (select-keys whoami [:account-id])
                      {:id rehearsal-id})]
    (entry-delete-by-rehearsal-id! db params)
    (rehearsal-delete-by-id! db params)))

(defn find-all [db whoami]
  (rehearsal-select db whoami))

(defn find-open [db whoami]
  (first (rehearsal-select-open db whoami)))

(defn entry-add! [db whoami entry]
  (let [rehearsal-id (-> (find-open db whoami) :id)]
    (when (nil? rehearsal-id)
      (throw (ex-info "Must open a rehearsal before making entries"
                      {:type :data-model-violation})))
    (when-not (= rehearsal-id (:rehearsal-id entry))
      (throw (ex-info (str "Must make entries in the open rehearsal (" rehearsal-id ")")
                      {:type :data-model-violation}))))
  (entry-insert<! db (assoc entry :account-id (:account-id whoami))))

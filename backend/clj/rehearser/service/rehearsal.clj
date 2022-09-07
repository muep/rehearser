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

(defn find-by-id [db whoami id]
  (let [tbl (rehearsal-select-by-id db {:account-id (:account-id whoami)
                                        :id id})
        {:keys [account-id
                rehearsal-id
                rehearsal-start-time
                rehearsal-end-time
                rehearsal-duration
                rehearsal-title
                rehearsal-description]} (first tbl)]
    (-> {:id rehearsal-id
         :account-id account-id
         :start-time rehearsal-start-time
         :end-time rehearsal-end-time
         :duration rehearsal-duration
         :title rehearsal-title
         :description rehearsal-description}
        (assoc :entries (map #(select-keys % [:id
                                              :account-id
                                              :rehearsal-id
                                              :exercise-id
                                              :variant-id
                                              :entry-time
                                              :remarks])
                             tbl)))))

(defn entry-add! [db whoami entry]
  (let [rehearsal-id (-> (find-open db whoami) :id)]
    (when (nil? rehearsal-id)
      (throw (ex-info "Must open a rehearsal before making entries"
                      {:type :data-model-violation})))
    (when-not (= rehearsal-id (:rehearsal-id entry))
      (throw (ex-info (str "Must make entries in the open rehearsal (" rehearsal-id ")")
                      {:type :data-model-violation}))))
  (entry-insert<! db (assoc entry :account-id (:account-id whoami))))

(defn entry-find-by-id [db whoami entry-id]
  (-> (entry-select db {:account-id (:account-id whoami)
                        :id entry-id})
      first))

(defn entry-delete-by-id! [db whoami entry-id]
  (let [rehearsal-id (-> (find-open db whoami) :id)]
    (when (nil? rehearsal-id)
      (throw (ex-info "Only possible to remove from an open rehearsal"
                      {:type :data-model-violation})))
    (let [result (entry-delete! db {:account-id (:account-id whoami)
                                    :rehearsal-id rehearsal-id
                                    :id entry-id})]
      (when (= 0 result)
        (throw (ex-info
                (str "No entry " entry-id " in the open rehearsal " rehearsal-id)
                {:type :not-found}))))))

(ns rehearser.service.import
  (:require
   [rehearser.service.exercise :as exercise]
   [rehearser.service.variant :as variant]
   [rehearser.service.rehearsal :as rehearsal])
  (:import
   (org.postgresql.util PSQLException)))

(defn import-data [db whoami {:keys [exercises variants rehearsals entries]}]
  (let [variant-ids (reduce (fn [ids {:keys [id title description]}]
                              (let [new-id (:id (or (variant/find-by-title db whoami title)
                                                    (variant/add! db whoami {:title title
                                                                             :description description})))]
                                (assoc ids id new-id)))
                            {}
                            variants)
        exercise-ids (reduce (fn [ids {:keys [id title description]}]
                              (let [new-id (:id (or (exercise/find-by-title db whoami title)
                                                    (exercise/add! db whoami {:title title
                                                                              :description description})))]
                                (assoc ids id new-id)))
                             {}
                             exercises)

        rehearsal-ids (reduce (fn [ids {:keys [description start-time duration title id]}]
                                (let [new-id (:id (rehearsal/insert-rehearsal! db whoami
                                                                               {:title title
                                                                                :description description
                                                                                :start-time start-time
                                                                                :duration duration}))]
                                  (assoc ids id new-id)))
                              {}
                              rehearsals)
        entry-ids (reduce (fn [ids {:keys [id entry-time remarks exercise-id rehearsal-id variant-id]}]
                            (let [new-id (:id (rehearsal/insert-entry! db whoami {:remarks remarks
                                                                                  :entry-time entry-time
                                                                                  :exercise-id (get exercise-ids exercise-id)
                                                                                  :rehearsal-id (get rehearsal-ids rehearsal-id)
                                                                                  :variant-id (get variant-ids variant-id)}))]
                              (assoc ids id new-id)))
                          {}
                          entries)]
    {:variant-ids variant-ids
     :exercise-ids exercise-ids
     :rehearsal-ids rehearsal-ids
     :entry-ids entry-ids}))

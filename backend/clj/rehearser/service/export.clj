(ns rehearser.service.export
  (:require
   [rehearser.db :refer [def-db-fns]])
  (:import (java.time Instant)))

(def-db-fns "rehearser/export.sql")

(defn export-account [db-conn account-id]
  ;; Validate account exists
  (let [account (account-for-export db-conn {:account-id account-id})]
    (when (nil? account)
      (throw (ex-info "Account not found" {:type :account-not-found :account-id account-id})))

    ;; Fetch all data for the account
    (let [exercises (exercises-for-export db-conn {:account-id account-id})
          variants (variants-for-export db-conn {:account-id account-id})
          rehearsals (rehearsals-for-export db-conn {:account-id account-id})
          entries (entries-for-export db-conn {:account-id account-id})
          exported-at (Instant/now)]

      {:version 1
       :exported-at exported-at
       :account account
       :exercises exercises
       :variants variants
       :rehearsals rehearsals
       :entries entries})))

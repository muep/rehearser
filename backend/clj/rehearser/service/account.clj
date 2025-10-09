(ns rehearser.service.account
  (:require [hugsql.core :refer [def-db-fns]])
  (:import (org.springframework.security.crypto.bcrypt BCrypt)
           (java.sql SQLException)
           (org.postgresql.util PSQLException)))

(def-db-fns "rehearser/account.sql")

(def account-regex #"[a-zA-Z][a-zA-Z0-9]{3,29}")

(defn normalized-name [name]
  (if (re-matches account-regex name)
    (.toLowerCase name)
    (throw (ex-info (str "Account name does not match regexp "
                         account-regex)
                    {:type :account-name-format}))))

(defn create-account! [db username password]
  (let [account
        (account-create! db {:name username
                             :pwhash (BCrypt/hashpw password (BCrypt/gensalt 12))})]
    (when (not (nil? account))
      (account-default-variant! db {:account-id (:id account)}))
    account))

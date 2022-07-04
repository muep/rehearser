(ns rehearser.service.account
  (:require [jeesql.core :refer [defqueries]])
  (:import (org.springframework.security.crypto.bcrypt BCrypt)
           (java.sql SQLException)
           (org.postgresql.util PSQLException)))

(defqueries "rehearser/account.sql")

(def account-regex #"[a-zA-Z][a-zA-Z0-9]{3,29}")

(defn normalized-name [name]
  (if (re-matches account-regex name)
    (.toLowerCase name)
    (throw (ex-info (str "Account name does not match regexp "
                         account-regex)
                    {:type :account-name-format}))))

(defn create-account! [db username password]
  (account-create<! db {:name username
                        :pwhash (BCrypt/hashpw password (BCrypt/gensalt 12))}))

(defn valid-username? [name]
  (cond
    (<= 4 (count name)) false
    ))

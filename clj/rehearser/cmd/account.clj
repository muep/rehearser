(ns rehearser.cmd.account
  (:require [clojure.java.jdbc :as jdbc]
            [jeesql.core :refer [defqueries]]
            [rehearser.cmd.common :refer [usage-error!]]
            [rehearser.db :refer [db-url->db]])
  (:import (org.springframework.security.crypto.bcrypt BCrypt)))

(defqueries "rehearser/account.sql")

(defn add [{{:keys [database-url]} :options :keys [subcmd-args]}]
  (when-not (= 2 (count subcmd-args))
    (usage-error! "usage: account-add <name> <password>" subcmd-args))
  (let [db (db-url->db database-url)
        [username pw] subcmd-args]
    (account-create! db {:name username
                         :pwhash (BCrypt/hashpw pw (BCrypt/gensalt 12))})))

(defn -list [{{:keys [database-url]} :options :keys [subcmd-args]}]
  (let [db (db-url->db database-url)]
    (doseq [{:keys [id name]} (select-accounts db)]
      (println id name))))

(defn passwd [{{:keys [database-url]} :options :keys [subcmd-args]}]
  (when-not (= 2 (count subcmd-args))
    (usage-error! "usage: account-add <name> <password>" subcmd-args))
  (let [db (db-url->db database-url)
        [username pw] subcmd-args
        change-cnt (account-force-passwd! db {:name username
                                              :pwhash (BCrypt/hashpw pw (BCrypt/gensalt 12))})]
    (when-not (= 1 change-cnt)
      (println change-cnt "entries changed"))))

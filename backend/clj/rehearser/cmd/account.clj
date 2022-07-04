(ns rehearser.cmd.account
  (:require [clojure.java.jdbc :as jdbc]
            [jeesql.core :refer [defqueries]]
            [rehearser.cmd.common :refer [usage-error!]]
            [rehearser.service.account :as service])
  (:import (org.springframework.security.crypto.bcrypt BCrypt)))

(defqueries "rehearser/account.sql")

(defn add [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (when-not (= 2 (count subcmd-args))
    (usage-error! "Expected exactly two arguments"
                  "usage: rehearser account-add <name> <password>"))
  (let [db {:connection-uri jdbc-url}
        [username pw] subcmd-args]
    (if-let [account-id (-> (service/create-account! db
                                                     (service/normalized-name username)
                                                     pw)
                            :id)]
      (println "Account" username "created with id:" account-id)
      (println "Account named" username "already existed, nothing was changed"))))

(defn -list [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (let [db {:connection-uri jdbc-url}]
    (doseq [{:keys [id name]} (select-accounts db)]
      (println id name))))

(defn passwd [{{:keys [jdbc-url]} :options :keys [subcmd-args]}]
  (when-not (= 2 (count subcmd-args))
    (usage-error! "Expected exactly two arguments"
                  "usage: rehearser account-passwd <name> <password>"))
  (let [db {:connection-uri jdbc-url}
        [username pw] subcmd-args
        change-cnt (account-force-passwd! db {:name username
                                              :pwhash (BCrypt/hashpw pw (BCrypt/gensalt 12))})]
    (when-not (= 1 change-cnt)
      (println change-cnt "entries changed"))))

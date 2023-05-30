(ns rehearser.test-db
  (:require
    [clojure.java.jdbc :as jdbc]

    [rehearser.db :as db]))

(def postgres-host (or (System/getenv "POSTGRES_HOST") "localhost"))

(def admin-db (str "jdbc:postgresql://" postgres-host ":5432/postgres?user=postgres&password=postgres"))
(def template-db-as-tester (str "jdbc:postgresql://" postgres-host ":5432/rehearser_template?user=rehearser_test&password=rehearser_test"))

(def ^:dynamic test-db nil)

(defn admin-execute! [cmd]
  (jdbc/execute! admin-db cmd {:transaction? false}))

(defn prepare-template-db! []
  (admin-execute! "create role rehearser_test with login password 'rehearser_test'")
  (admin-execute! "create database rehearser_template with owner = rehearser_test;")
  (db/reset template-db-as-tester))

(defn cleanup-template-db! []
  (admin-execute! "drop database if exists rehearser_template;")
  (admin-execute! "drop role if exists rehearser_test;"))

(defn wrap-prepared-template-db! [app]
  ;; Just in case we have some leftovers from previous runs
  (cleanup-template-db!)
  (prepare-template-db!)
  (try
    (app)
    (finally
      (cleanup-template-db!))))

(def db-ctr (atom 0))

(defn make-disposable-test-db! []
  (let [db-name (str "rehearser_test_" (swap! db-ctr inc))]
    (admin-execute! (str "drop database if exists " db-name ";"))
    (admin-execute! (str "create database " db-name " with "
                         "owner = rehearser_test "
                         "template = rehearser_template;"))
    {:cleanup (fn []
                (admin-execute! (str "drop database " db-name)))
     :jdbc-url (str "jdbc:postgresql://" postgres-host ":5432/" db-name "?user=rehearser_test&password=rehearser_test")}))

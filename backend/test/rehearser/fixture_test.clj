(ns rehearser.fixture-test
  (:require
    [clojure.test :as t]
    [clojure.java.jdbc :as jdbc]
    [rehearser.test-db :as test-db]
    [rehearser.fixture :as fixture]))

(t/use-fixtures :each fixture/fixture)

(t/deftest database-is-set
  (t/testing "The test-db var is at least set to something"
    (t/is (not (nil? test-db/test-db)))))

(t/deftest expected-content
  (t/testing "The test database has at least some known database objects"
    (let [rows (jdbc/query test-db/test-db "select * from account;")]
      (t/is (= rows [])))))
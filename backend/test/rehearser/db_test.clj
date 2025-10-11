(ns rehearser.db-test
  (:require
   [clojure.test :as t]
   [next.jdbc :as jdbc]
   [rehearser.db :as db]
   [rehearser.test-db :refer [test-db]]
   [rehearser.fixture :refer [fixture]])
  (:import
   (java.time Instant)
   (java.time.temporal ChronoUnit)))

(t/use-fixtures :each fixture)

(t/deftest jdbc-db-timetamp-bridge-test
  (let [nw (-> (Instant/now)
               (.truncatedTo ChronoUnit/MICROS))]
    (t/is (= nw (-> (jdbc/execute! test-db ["select ?::timestamptz as now" nw])
                    first
                    :now)))))

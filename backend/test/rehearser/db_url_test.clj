(ns rehearser.db-url-test
  (:require
   [clojure.test :as t]
   [rehearser.db-url :refer [libpq->jdbc]]))

(t/deftest libpq->jdbc-test
  (t/is (=
         "jdbc:postgresql://localhost:5432/rehearser?user=rehearser&password=rehearser"
         (libpq->jdbc "postgres://rehearser:rehearser@localhost:5432/rehearser"))
        "Default URL")
  (t/is (=
         "jdbc:postgresql://192.168.4.1:5499/reh_db?user=reh_user&password=reh_pw"
         (libpq->jdbc "postgres://reh_user:reh_pw@192.168.4.1:5499/reh_db"))
        "Non-default value in every parameter"))

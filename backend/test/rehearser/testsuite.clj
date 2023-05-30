(ns rehearser.testsuite
  (:require [clojure.test :as test]
            [rehearser.test-db :as test-db]

            [rehearser.account-test]
            [rehearser.db-url-test]
            [rehearser.fixture-test]
            [rehearser.malli-test]
            [rehearser.handler-test]
            [rehearser.handler-progressive-test]))

(defn- test-pattern [kw]
  (re-pattern (str "rehearser\\..*" kw ".*-test")))

(defn -main [& args]
  (test-db/wrap-prepared-template-db!
    (fn []
      (if (empty? args)
        (test/run-all-tests #"rehearser\..*-test")
        (apply test/run-all-tests (map test-pattern
                                       args))))))

(ns rehearser.testsuite
  (:require [clojure.test :as test]
            [rehearser.db-url-test]))

(defn- test-pattern [kw]
  (re-pattern (str "rehearser\\..*" kw ".*-test")))

(defn -main [& args]
  (if (empty? args)
    (test/run-all-tests #"rehearser\..*-test")
    (apply test/run-all-tests (map test-pattern
                                   args))))

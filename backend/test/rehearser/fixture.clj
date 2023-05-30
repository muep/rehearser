(ns rehearser.fixture
  (:require
    [rehearser.test-db :as test-db]))

(defn fixture [test-fn]
  (let [{:keys [jdbc-url cleanup]} (test-db/make-disposable-test-db!)]
    (try
      (with-bindings
        {#'rehearser.test-db/test-db jdbc-url}
        (test-fn))
      (finally
        (cleanup)))))

(ns rehearser.testsuite
  (:require
   [eftest.runner :as eftest]
   [rehearser.test-db :as test-db]
   [rehearser.test-util :refer [test-time-reporter]]))

(defn- test-pattern [kw]
  (re-pattern (str "rehearser\\..*" kw ".*")))

(defn- matches-some-pattern [v patterns]
  (let [m (meta v)
        name (str (ns-name (:ns m)) "/" (:name m))]
    (some #(re-find % name) patterns)))

(defn -main [& args]
  (test-db/wrap-prepared-template-db!
    (fn []
      (let [patterns (if (seq args)
                      (mapv test-pattern args)
                      [#"rehearser\..*-test"])
            {:keys [reporter timings]} (test-time-reporter
                                        (if (nil? (System/console))
                                          eftest.report.pretty/report
                                          eftest.report.progress/report))]
        (as-> (eftest/find-tests "backend/test") $
          (filterv #(matches-some-pattern % patterns) $)
          (eftest/run-tests
           $
           {:fail-fast? true
            :capture-output? true
            :multithread? true
            :report reporter}))

        (println "\nTests with long duration:")
        (doseq [[v ms] (->> @timings (filter #(< 1000 (second %))) (sort-by second))]
          (println (format "%-60s %8.0f ms" (clojure.string/replace v #"^rehearser" "r") ms)))))))

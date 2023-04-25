(ns rehearser.malli-test
  (:require
   [clojure.test :as t]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]
   [rehearser.malli :refer [Timestamp malli-options]])
  (:import
   (java.time Instant)))

(t/deftest validate-instant-via-var
  (t/is (nil? (-> (m/explain Timestamp
                             (Instant/now))
                  me/humanize))))

(t/deftest instant-coerce-via-var
  (t/is (= (Instant/ofEpochSecond 1682359853)
           (m/coerce Timestamp
                     1682359853
                     mt/json-transformer))))

(t/deftest validate-instant-via-registry
  (t/is (nil? (-> (m/explain :timestamp
                             (Instant/now)
                             malli-options)
                  me/humanize))))

(t/deftest instant-coerce-via-registry
  (t/is (= (Instant/ofEpochSecond 1682359853)
           (m/coerce :timestamp
                     1682359853
                     mt/json-transformer
                     malli-options))))

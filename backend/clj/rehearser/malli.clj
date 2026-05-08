(ns rehearser.malli
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]
            [malli.registry :as mr]
            [malli.transform :as mt])
  (:import (java.time Instant)))

(defn number->instant [num]
  (if (number? num)
    (-> num Instant/ofEpochSecond)
    ::m/invalid))

(defn instant->number [^Instant i]
  (-> i .getEpochSecond))

(def Timestamp [(m/-simple-schema {:pred #(instance? Instant %)})
                {:decode {:json number->instant}
                 :encode {:json instant->number}}])

(def schema-registry
  (-> m/default-registry
      (mr/composite-registry {:timestamp Timestamp})))

(def malli-options {:registry schema-registry})

(def Metadata
  (m/schema [:map {:closed true}
             [:id int?]
             [:account-id int?]]))

(defn save->update [save-schema]
  (m/schema
   [:and
    (mu/optional-keys save-schema)
    [:fn
     {:error/fn (constantly "At least one update is required")}
     (fn [m] (not (empty? m)))]]))

(defn decode-and-explain [schema value]
  (let [decoded (m/decode schema value mt/json-transformer)
        valid (m/validate schema decoded)]
    (if valid
      decoded
      (let [explanation (m/explain schema decoded)
            humanized (me/humanize explanation)]
        (throw (ex-info "Data did not match schema"
                        {:type :coercion-error
                         :explanation explanation
                         :humanized humanized}))))))


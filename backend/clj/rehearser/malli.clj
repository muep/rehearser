(ns rehearser.malli
  (:require [malli.core :as m]
            [malli.util :as mu]
            [malli.registry :as mr])
  (:import (java.time Instant)))

(defn number->instant [num]
  (-> num Instant/ofEpochSecond))

(defn instant->number [i]
  (-> i .toEpochSecond))

(def Timestamp [(m/-simple-schema {:type :int
                                   :pred #(instance? Instant %)})
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

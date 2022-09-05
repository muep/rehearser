(ns rehearser.malli
  (:require [malli.core :as m]
            [malli.util :as mu]
            [malli.error :as me]))

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

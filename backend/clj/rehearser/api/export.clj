(ns rehearser.api.export
  (:require
   [rehearser.service.export :as export-service]
   [rehearser.malli :refer [Timestamp]]
   [malli.core :as m]
   [malli.util :as mu]
   [ring.util.response :as ring-resp])
  (:import (java.time Instant)))

(defn- instant->unix-seconds [instant]
  (.getEpochSecond ^Instant instant))

;; Malli schemas for export data structure
(def AccountExport
  (m/schema [:map {:closed true}
             [:id int?]
             [:name string?]]))

(def ExerciseExport
  (m/schema [:map {:closed true}
             [:id int?]
             [:title string?]
             [:description string?]]))

(def VariantExport
  (m/schema [:map {:closed true}
             [:id int?]
             [:title string?]
             [:description string?]]))

(def RehearsalExport
  (m/schema [:map {:closed true}
             [:id int?]
             [:title string?]
             [:description string?]
             [:start-time int?]
             [:duration int?]]))

(def EntryExport
  (m/schema [:map {:closed true}
             [:id int?]
             [:rehearsal-id int?]
             [:exercise-id int?]
             [:variant-id int?]
             [:entry-time int?]
             [:remarks [:maybe string?]]]))

(def ExportResponse
  (m/schema [:map {:closed true}
             [:version int?]
             [:exported-at int?]
             [:account AccountExport]
             [:exercises [:sequential ExerciseExport]]
             [:variants [:sequential VariantExport]]
             [:rehearsals [:sequential RehearsalExport]]
             [:entries [:sequential EntryExport]]]))

(defn- convert-timestamps [data]
  (cond
    (instance? Instant data)
    (instant->unix-seconds data)

    (map? data)
    (into {} (map (fn [[k v]] [k (convert-timestamps v)]) data))

    (sequential? data)
    (mapv convert-timestamps data)

    :else
    data))

(defn export-handler [{:keys [db whoami]}]
  (let [account-id (:account-id whoami)
        export-data (export-service/export-account db account-id)

        ;; Convert Instant objects to Unix epoch seconds
        json-data (convert-timestamps export-data)

        ;; Generate filename with ISO-8601 timestamp
        timestamp-str (.format (java.time.format.DateTimeFormatter/ISO_INSTANT) (Instant/now))
        filename (str "rehearser-export-" timestamp-str ".json")]

    {:status 200
     :body json-data
     :headers {"Content-Disposition" (str "attachment; filename=\"" filename "\"")}}))

(def routes
  [["" {:get {:handler export-handler
              :responses {200 {:body ExportResponse}}}}]])

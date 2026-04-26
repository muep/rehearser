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
             [:start-time Timestamp]
             [:duration [:maybe integer?]]]))

(def EntryExport
  (m/schema [:map {:closed true}
             [:id int?]
             [:rehearsal-id int?]
             [:exercise-id int?]
             [:variant-id int?]
             [:entry-time Timestamp]
             [:remarks [:maybe string?]]]))

(def ExportStructure
  (m/schema [:map {:closed true}
             [:version int?]
             [:exported-at Timestamp]
             [:account AccountExport]
             [:exercises [:sequential ExerciseExport]]
             [:variants [:sequential VariantExport]]
             [:rehearsals [:sequential RehearsalExport]]
             [:entries [:sequential EntryExport]]]))

(defn export-handler [{:keys [db whoami]}]
  (let [account-id (:account-id whoami)
        json-data (export-service/export-account db account-id)

        ;; Generate filename with ISO-8601 timestamp
        timestamp-str (.format (java.time.format.DateTimeFormatter/ISO_INSTANT) (Instant/now))
        filename (str "rehearser-export-" timestamp-str ".json")]

    {:status 200
     :body json-data
     :headers {"Content-Disposition" (str "attachment; filename=\"" filename "\"")}}))

(def routes
  [["/export" {:get {:handler export-handler
                     :responses {200 {:body ExportStructure}}}}]])

(ns rehearser.ui.import
  (:require
   [hiccup.core :as h]
   [jsonista.core :as json]
   [rehearser.api.export :refer [ExportStructure]]
   [rehearser.malli :as malli]
   [rehearser.service.import :as import-service]
   [rehearser.ui.common :as common-ui]))

(def object-mapper (json/object-mapper {:decode-key-fn true}))

(defn import-form [url-prefix]
  [:div
   [:h1 "Import Data"]
   [:p "Upload a Rehearser export file to import your data."]
   [:form {:method "post" :enctype "multipart/form-data"}
    [:input {:type "file" :name "file" :id "import-file-input" :required true :accept ".json"}]
    [:button {:type "submit"} "Import"]
    [:p {:class "note"} "Note: This will import exercises, variants, rehearsals, and entries. Duplicates will be skipped."]
    [:a {:href (str url-prefix "/index.html")} "Cancel and return to main page"]]])

(defn import-handler [{:keys [db whoami url-prefix]
                       {:keys [multipart]} :parameters}]
  (let [file-content (-> multipart :file first :tempfile slurp)
        parsed-data (json/read-value file-content object-mapper)
        validated-data (malli/decode-and-explain ExportStructure parsed-data)
        result (import-service/import-data db whoami validated-data)]
    {:status 303
     :headers {"location" (str url-prefix "/index.html")}}))

(defn html [{{:keys [account-name] :as whoami} :whoami
             :keys [url-prefix]
             :as req}]
  {:status 200
   :body (str
          "<!DOCTYPE html>"
          (h/html
              [:html
               (common-ui/head url-prefix "import")
               [:body
                (if (nil? account-name)
                  [:div
                   [:p "You need to be logged in to import data."]
                   [:a {:href (str url-prefix "/index.html")} "Log in"]]
                  (import-form url-prefix))]]))})

(def routes
  [["/import.html"
    {:get {:handler html
           :allow-anonymous? false}
     :post {:handler import-handler
            :parameters {:multipart {:file any?}}
            :allow-anonymous? false}}]])

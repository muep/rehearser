;; Functions for experimenting with middlewares
(ns rehearser.exp)

(defn params-get [{:keys [params]}]
  {:status 200
   :body params})

(def params-post params-get)

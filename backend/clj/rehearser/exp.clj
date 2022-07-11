;; Functions for experimenting with middlewares
(ns rehearser.exp)

(defn params-get [{:keys [params whoami]}]
  {:status 200
   :body {:params params
          :whoami whoami}})

(def params-post params-get)

(defn fail [req]
  (throw (ex-info "Intentional failure in request processing" {})))

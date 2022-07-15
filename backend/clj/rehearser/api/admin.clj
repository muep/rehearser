(ns rehearser.api.admin)

(def routes ["/status" {:get (fn [_] {:status 200})}])

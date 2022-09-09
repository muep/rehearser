(ns rehearser.response)

(defn get-one [result]
  (if-let [body (first result)]
    {:status 200
     :body body}
    {:status 404
     :body "Resource was not found"}))

(defn modify-one [result]
  (if (<= 1 result)
    {:status 200
     :body (str result " items changed")}
    {:status 404
     :body "Not found, not changed"}))

(defn update-one [result]
  (if result
    {:status 200
     :body result}
    {:status 404
     :body "Not found, not changed"}))

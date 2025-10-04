(ns rehearser.test-util
  (:require
   [jsonista.core :as json]
   [ring.mock.request :as mock])
  (:import (java.io ByteArrayInputStream)))

(def object-mapper (json/object-mapper {:decode-key-fn true}))

(defn read-json-value [v]
  (json/read-value v object-mapper))

(defn handler-with-local-cookies [handler]
  (let [cookies (atom {})]
    (fn [req]
      (let [req-with-cookies (reduce (fn [req [cookie-name cookie-value]]
                                       (mock/cookie req cookie-name cookie-value))
                                     req
                                     @cookies)
            response (handler req-with-cookies)]
        (when-let [set-cookie (first (get-in response [:headers "Set-Cookie"]))]
          (let [[cookie-name cookie-val] (clojure.string/split set-cookie #"=" 2)]
            (swap! cookies assoc cookie-name (first (clojure.string/split cookie-val #";")))))
        response))))

(defn post-form-request [uri params]
  (let [body-payload (ring.util.codec/form-encode params)]
    {:request-method :post
     :uri            uri
     :headers        {"accept"       "application/json"
                      "content-type" "application/x-www-form-urlencoded"
                      "content-length" (-> body-payload .length str)}
     :body (-> body-payload .getBytes ByteArrayInputStream.)}))

(defn post-json-request [uri body-map]
  (let [body-text (json/write-value-as-string body-map)]
    {:request-method :post
     :uri            uri
     :headers        {"accept" "application/json"
                      "content-type" "application/json"
                      "content-length" (-> body-text .length str)}
     :body (-> body-text .getBytes ByteArrayInputStream.)}))

(defn put-json-request [uri body-map]
  (let [body-text (json/write-value-as-string body-map)]
    {:request-method :put
     :uri            uri
     :headers        {"accept" "application/json"
                      "content-type" "application/json"
                      "content-length" (-> body-text .length str)}
     :body (-> body-text .getBytes ByteArrayInputStream.)}))

(defn var-name [v]
  (let [m (meta v)]
    (str (ns-name (:ns m)) "/" (:name m))))

(defn test-time-reporter [reporter]
  (let [timings (atom {})
        reporter (fn [{:keys [type var] :as m}]
                   (case type
                     :begin-test-var
                     (swap! timings assoc (var-name var) (System/nanoTime))

                     :end-test-var
                     (let [start (get @timings (var-name var))
                           elapsed (/ (- (System/nanoTime) start) 1e6)]
                       (swap! timings assoc (var-name var) elapsed))

                     nil)
                   (reporter m))]
    {:reporter reporter
     :timings timings}))

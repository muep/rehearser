(ns rehearser.account-test
  (:require
    [clojure.test :as t]
    [rehearser.fixture :refer [fixture]]
    [rehearser.test-db :refer [test-db]]

    [crypto.random :as random]
    [jsonista.core :as json]

    [rehearser.http-service :as http-service])
  (:import (java.io ByteArrayInputStream)))

(t/use-fixtures :each fixture)

(defn post-json-text-request [uri body-text]
  {:request-method :post
   :uri uri
   :headers {"accept" "application/json"
             "content-type" "application/json"
             "content-length" (-> body-text .length str)}
   :body (-> body-text .getBytes ByteArrayInputStream.)})

(defn post-form-request [uri params]
  (let [body-payload (ring.util.codec/form-encode params)]
    {:request-method :post
     :uri            uri
     :headers        {"accept"       "application/json"
                      "content-type" "application/x-www-form-urlencoded"
                      "content-length" (-> body-payload .length str)}
     :body (-> body-payload .getBytes ByteArrayInputStream.)}))

(t/deftest login-required-test
  (let [app (:handler (http-service/make-app test-db (random/bytes 16) nil nil))
        whoami-response (app {:request-method :get
                              :uri            "/api/whoami"
                              :body ""})]
    (t/is (= (:status whoami-response) 200))
    (t/is (= (-> whoami-response :body json/read-value :account-id) nil))))

(t/deftest signup-test
  (let [app (:handler (http-service/make-app test-db (random/bytes 16) nil nil))]
    (let [response (app (post-form-request
                          "/api/signup"
                          {:username "bobb" :password "s3kret"}))]
      (t/is (= (:status response) 303))
      (t/is (= (-> response :headers (get "Location")) "../login.html")))

    (let [response (app (post-form-request
                          "/api/login"
                          {:username "bobb" :password "badpw"}))]
      (t/is (= (:status response) 303))
      ;; This is pretty bad API, basically no indication of success
      ;; or failure except for the redirect URI
      (t/is (= (-> response :headers (get "Location")) "../login.html")))

    (let [response (app (post-form-request
                          "/api/login"
                          {:username "bobb" :password "s3kret"}))]
      (t/is (= (:status response) 303))
      (t/is (= (-> response :headers (get "Location")) "../index.html")))))

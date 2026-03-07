(ns rehearser.redirect-to-index-test
  (:require
    [clojure.test :as t]
    [rehearser.fixture :refer [fixture]]
    [rehearser.test-db :refer [test-db]]

    [crypto.random :as random]
    [ring.mock.request :as mock]

    [rehearser.http-service :as http-service]
    [rehearser.test-util :refer [handler-with-local-cookies
                                 read-json-value]])
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

(t/deftest redirect-anonymous-user-to-index
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies)]

    ;; Initially should not get an account id from whoami
    (let [whoami-response (app {:request-method :get
                                :uri            "/api/whoami"
                                :body ""})]
      (t/is (= (:status whoami-response) 200))
      (t/is (nil? (-> whoami-response :body read-json-value :account-id))))

    ;; Let's make an account from scratch
    (let [response (app (post-form-request
                         "/api/signup"
                         {:username "bobb" :password "s3kret"}))]
      (t/is (= (:status response) 303))
      (t/is (= (-> response :headers (get "Location")) "../index.html")))

    ;; Still should not get an account id from whoami
    (let [whoami-response (app {:request-method :get
                                :uri            "/api/whoami"
                                :body ""})]
      (t/is (= (:status whoami-response) 200))
      (t/is (nil? (-> whoami-response :body read-json-value :account-id))))


    ;; Requests to the server-rendered UI should redirect to index
    (let [tunes-response (app {:request-method :get
                               :uri            "/tunes.html"
                               :body ""})]
      (t/is (= 303 (:status tunes-response)))
      (t/is (= "/index.html" (-> tunes-response :headers (get "location")))))

    ;; Log in with the correct password
    (let [response (app (post-form-request
                         "/api/login"
                         {:username "bobb" :password "s3kret"}))]
      (t/is (= (:status response) 303))
      (t/is (= (-> response :headers (get "Location")) "../index.html")))

    ;; Requests to the server-rendered UI should now not redirect
    (let [tunes-response (app {:request-method :get
                               :uri            "/tunes.html"
                               :body ""})]
      (t/is (= 200 (:status tunes-response))))))

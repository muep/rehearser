(ns rehearser.account-test
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

(t/deftest login-required-test
  (let [app (:handler (http-service/make-app test-db (random/bytes 16) nil nil))
        whoami-response (app {:request-method :get
                              :uri            "/api/whoami"
                              :body ""})]
    (t/is (= (:status whoami-response) 200))
    (t/is (= (-> whoami-response :body read-json-value :account-id) nil))))

(t/deftest signup-test
  (let [app (-> (http-service/make-app test-db (random/bytes 16) nil nil)
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
      (t/is (= (-> response :headers (get "Location")) "../login.html")))

    ;; Attempt logging in with a bad password
    (let [response (app (post-form-request
                         "/api/login"
                         {:username "bobb" :password "badpw"}))]
      (t/is (= (:status response) 303))
      ;; This is pretty bad API, basically no indication of success
      ;; or failure except for the redirect URI
      (t/is (= (-> response :headers (get "Location")) "../login.html")))

    ;; Still should not get an account id from whoami
    (let [whoami-response (app {:request-method :get
                                :uri            "/api/whoami"
                                :body ""})]
      (t/is (= (:status whoami-response) 200))
      (t/is (nil? (-> whoami-response :body read-json-value :account-id))))

    ;; Log in with the correct password
    (let [response (app (post-form-request
                         "/api/login"
                         {:username "bobb" :password "s3kret"}))]
      (t/is (= (:status response) 303))
      (t/is (= (-> response :headers (get "Location")) "../index.html")))

    ;; Now the API should report an user id
    (let [whoami-response (app {:request-method :get
                                :uri            "/api/whoami"
                                :body ""})]
      (t/is (= (:status whoami-response) 200))
      (t/is (integer? (-> whoami-response :body read-json-value :account-id))))

    ;; Try logging out
    (let [logout-response (app {:request-method :post
                                :uri            "/api/logout"
                                :body ""})]
      (t/is (= (:status logout-response) 303))
      (t/is (= (-> logout-response :headers (get "Location")) "../login.html")))

    ;; Now it should not have an id any more
    (let [whoami-response (app {:request-method :get
                                :uri            "/api/whoami"
                                :body ""})]
      (t/is (= (:status whoami-response) 200))
      (t/is (nil? (-> whoami-response :body read-json-value :account-id))))))

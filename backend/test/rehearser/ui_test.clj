(ns rehearser.ui-test
  (:require
   [clojure.test :as t]
   [ring.mock.request :as mock]
   [rehearser.http-service :as http-service]
   [rehearser.test-db :refer [test-db]]
   [rehearser.fixture :as fixture]
   [crypto.random :as random]))

(t/use-fixtures :each rehearser.fixture/fixture)

(t/deftest redirect-to-index-test
  (let [app (:handler (http-service/make-app test-db (random/bytes 16) "" nil nil))
        response (app (mock/request :get "/"))]
    (t/is (= (:status response) 302))
    (t/is (= (get-in response [:headers "Location"]) "/index.html"))))

(t/deftest redirect-to-index-test-with-url-prefix
  (let [app (:handler (http-service/make-app test-db (random/bytes 16) "/reh" nil nil))]
    (let [response (app (mock/request :get "/reh"))]
      (t/is (= (:status response) 302))
      (t/is (= (get-in response [:headers "Location"]) "/reh/index.html")))
    (let [response (app (mock/request :get "/reh/"))]
      (t/is (= (:status response) 302))
      (t/is (= (get-in response [:headers "Location"]) "/reh/index.html")))))

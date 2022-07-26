(ns rehearser.router-test
  (:require
   [clojure.test :as t]
   [reitit.core :as r]
   [rehearser.api.exercise :as exercise-api]
   [rehearser.http-service :refer [make-router]]))

(t/deftest router-exercise-test
  (let [router (make-router nil nil nil)]
    (t/is (-> router
              (r/match-by-path "/api/exercise")
              :data
              :get
              :handler
              (= exercise-api/get-exercises)))

    (t/is (-> router
              (r/match-by-path "/api/exercise")
              :data
              :post
              :handler
              (= exercise-api/post-exercise!)))

    (t/is (-> router
              (r/match-by-path "/api/exercise/4")
              :data
              :get
              :handler
              (= exercise-api/get-exercise)))

    (t/is (-> router
              (r/match-by-path "/api/exercise/8")
              :data
              :put
              :handler
              (= exercise-api/put-exercise!)))

    (t/is (-> router
              (r/match-by-path "/api/exercise/9")
              :data
              :delete
              :handler
              (= exercise-api/delete-exercise!)))))

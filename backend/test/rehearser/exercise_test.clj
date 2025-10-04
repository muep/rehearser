(ns rehearser.exercise-test
  (:require
    [clojure.test :as t]
    [rehearser.fixture :refer [fixture]]
    [rehearser.test-db :refer [test-db]]

    [crypto.random :as random]
    [jsonista.core :as json]
    [ring.mock.request :as mock]

    [rehearser.http-service :as http-service]
    [rehearser.test-util :refer [handler-with-local-cookies
                                 read-json-value
                                 post-form-request
                                 post-json-request
                                 put-json-request]]))

(t/use-fixtures :each fixture)

(t/deftest exercise-and-variant-crud-test
  (let [app (-> (http-service/make-app test-db (random/bytes 16) nil nil)
                :handler
                handler-with-local-cookies)]

    ;; sign up + login (form-based)
    (app (post-form-request "/api/signup" {:username "alice" :password "pw"}))
    (app (post-form-request "/api/login" {:username "alice" :password "pw"}))

    ;; initially empty
    (let [resp (app {:request-method :get :uri "/api/exercise"})]
      (t/is (= 200 (:status resp)))
      (t/is (empty? (read-json-value (:body resp)))))

    ;; create exercise (JSON)
    (let [resp (app (post-json-request "/api/exercise"
                                       {:title "Tune A"
                                        :description "First tune"}))]
      (t/is (= 200 (:status resp))))

    ;; list should now contain it
    (let [resp (app {:request-method :get :uri "/api/exercise"})
          exercises (read-json-value (:body resp))]
      (t/is (= 1 (count exercises)))
      (t/is (= "Tune A" (:title (first exercises)))))

    ;; update exercise
    (let [id (:id (first (read-json-value (:body (app {:request-method :get :uri "/api/exercise"})))))]
      (let [resp (app (put-json-request (str "/api/exercise/" id)
                                        {:title "Tune A updated"
                                         :description "Updated desc"}))]
        (t/is (= 200 (:status resp)))))

    ;; create variant (JSON)
    (let [resp (app (post-json-request "/api/variant"
                                       {:title "Piano"
                                        :description "Play on piano"}))]
      (t/is (= 200 (:status resp))))

    ;; list variants
    (let [resp (app {:request-method :get :uri "/api/variant"})
          variants (read-json-value (:body resp))]
      (t/is (= 2 (count variants)))
      (t/is (contains? (->> variants (map :title) set) "Piano" )))

    ;; update variant
    (let [id (:id (first (filter (fn [{:keys [title]}]
                                   (= title "Piano"))
                                 (read-json-value (:body (app {:request-method :get :uri "/api/variant"}))))))]
      (let [resp (app (put-json-request (str "/api/variant/" id)
                                        {:title "Guitar"
                                         :description "Play on guitar"}))]
        (t/is (= 200 (:status resp)))))

    ;; delete exercise
    (let [id (:id (first (read-json-value (:body (app {:request-method :get :uri "/api/exercise"})))))]
      (let [resp (app {:request-method :delete :uri (str "/api/exercise/" id)})]
        (t/is (= 200 (:status resp)))))

    ;; delete variant
    (let [id (:id (first (filter (fn [{:keys [title]}]
                                   (= title "Guitar"))
                                 (read-json-value (:body (app {:request-method :get :uri "/api/variant"}))))))]
      (let [resp (app {:request-method :delete :uri (str "/api/variant/" id)})]
        (t/is (= 200 (:status resp)))))

    ;; both lists should be empty again
    (let [resp (app {:request-method :get :uri "/api/exercise"})]
      (t/is (empty? (read-json-value (:body resp)))))
    (let [resp (app {:request-method :get :uri "/api/variant"})]
      (t/is (= 1 (count (read-json-value (:body resp))))))))

(ns rehearser.rehearsal-test
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

(defn handler-explaining-400 [handler]
  (fn [req]
    (let [{:keys [status] :as res} (handler req)]
      (when (= 400 status)
        (t/is (not= 400 status)
              (str "Unexpected error 400 from the handler: "
                   (-> res :body read-json-value :humanized))))
      res)))

(defn assert-status! [{:keys [status] :as response} expected-status]
  (when (not (= expected-status status))
    (t/is (= expected-status status)
          (str "Expected status " expected-status ", got " status ":"
               (-> response :body))))
  response)

(t/deftest rehearsal-and-entry-crud-test
  (let [app (-> (http-service/make-app test-db (random/bytes 16) nil nil)
                :handler
                handler-with-local-cookies
                handler-explaining-400)]

    ;; sign up + login
    (app (post-form-request "/api/signup" {:username "bobb" :password "pw"}))
    (app (post-form-request "/api/login" {:username "bobb" :password "pw"}))

    ;; Check that we are logged in
    (let [whoami-response (app {:request-method :get
                                :uri            "/api/whoami"
                                :body ""})]
      (t/is (= 200 (:status whoami-response)) "Expected 200 status from whoami")
      (t/is (integer? (-> whoami-response :body read-json-value :account-id))
            "No numeric account-id in whoami"))

    ;; initially empty
    (let [resp (app {:request-method :get :uri "/api/exercise"})]
      (t/is (= 200 (:status resp)))
      (t/is (empty? (read-json-value (:body resp)))))

    ;; add a couple of exercises
    (app (post-json-request "/api/exercise"
                            {:title "Tune A" :description "First tune"}))
    (app (post-json-request "/api/exercise"
                            {:title "Tune B" :description "Second tune"}))

    ;; fetch exercises
    (let [exercises (read-json-value
                      (:body (app {:request-method :get :uri "/api/exercise"})))
          ex-a (first exercises)
          ex-b (second exercises)

          ;; fetch the default variant (created at signup)
          variants (read-json-value
                     (:body (app {:request-method :get :uri "/api/variant"})))
          default-variant-id (:id (first variants))]

      ;; create rehearsal
      (let [resp (app (post-json-request "/api/rehearsal"
                                         {:start-time 1760107800
                                          :duration nil
                                          :title "Evening practice"
                                          :description "Worked on reels"}))]
        (t/is (= 200 (:status resp))))

      ;; list rehearsals
      (let [rehearsals (read-json-value
                         (:body (app {:request-method :get :uri "/api/rehearsal"})))
            rehearsal-id (->> rehearsals (filter :is-open) first :id)]

        ;; add entry for Tune A
        (let [resp (app (post-json-request (str "/api/rehearsal/" rehearsal-id "/entry")
                                           {:exercise-id (:id ex-a)
                                            :variant-id default-variant-id
                                            :entry-time 1760109900
                                            :remarks "Good progress on A"}))]
          (t/is (= 200 (:status resp))))

        ;; add entry for Tune B
        (let [resp (app (post-json-request (str "/api/rehearsal/" rehearsal-id "/entry")
                                           {:exercise-id (:id ex-b)
                                            :variant-id default-variant-id
                                            :entry-time 1760110500
                                            :remarks "Still rough on B"}))]
          (t/is (= 200 (:status resp))))

        ;; fetch rehearsal with embedded entries Also there's several
        ;; alternative ways to this data, so let's try poking at many
        ;; of those
        (let [entries-standalone (read-json-value
                                  (:body (app {:request-method :get
                                               :uri (str "/api/rehearsal/" rehearsal-id "/entry")})))
              rehearsal (read-json-value
                          (:body (app {:request-method :get
                                       :uri (str "/api/rehearsal/" rehearsal-id)})))
              entries (:entries rehearsal)]
          (t/is (= 2 (count entries-standalone)))
          (t/is (= (set (map :id entries-standalone)) (set (map :id entries))))

          ;; Quite a few details in the rehearsal

          ;; Check that it's still open
          (t/is (:is-open rehearsal))

          (t/is (= 2 (count entries)))
          ;; Names against the earlier defined names
          (t/is (= #{"Tune A" "Tune B"}
                   (->> entries
                        (map :exercise-id)
                        (map (fn [id]
                               (:title (first (filter #(= (:id %) id) exercises)))))
                        set)))

          ;; Names also from the response directly
          (t/is (= #{"Tune A" "Tune B"}
                   (->> entries
                        (map :exercise-title)
                        set)))
          (-> (app (put-json-request (str "/api/rehearsal/" rehearsal-id)
                                     {:duration 60}))
              (assert-status! 200))

          (let [rehearsal-after (read-json-value
                                 (:body (app {:request-method :get
                                              :uri (str "/api/rehearsal/" rehearsal-id)})))]
            ;; Check that it's not open
            (t/is (not (:is-open rehearsal-after)))

            ;; No other changes
            (t/is (= (dissoc rehearsal-after :duration :is-open)
                     (dissoc rehearsal :duration :is-open)))))))))

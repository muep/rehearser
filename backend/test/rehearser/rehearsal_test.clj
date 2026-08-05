(ns rehearser.rehearsal-test
  (:require
   [clojure.string :as string]
   [clojure.test :as t]
   [rehearser.fixture :refer [fixture]]
   [rehearser.test-db :refer [test-db]]

   [crypto.random :as random]

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
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
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

(t/deftest entry-duplication-test
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies
                handler-explaining-400)]

    ;; sign up + login
    (app (post-form-request "/api/signup" {:username "dupuser" :password "pw"}))
    (app (post-form-request "/api/login" {:username "dupuser" :password "pw"}))

    ;; add exercises and get default variant
    (app (post-json-request "/api/exercise"
                            {:title "Tune to Duplicate" :description "For testing"}))
    (let [exercises (read-json-value (:body (app {:request-method :get :uri "/api/exercise"})))
          variants (read-json-value (:body (app {:request-method :get :uri "/api/variant"})))
          exercise-id (:id (first exercises))
          variant-id (:id (first variants))

          ;; create rehearsal
          _ (app (post-json-request "/api/rehearsal"
                                     {:start-time 1760107800
                                      :duration nil
                                      :title "Duplicate Test Rehearsal"
                                      :description "Testing duplication"}))

          ;; locate the open rehearsal
          rehearsals (read-json-value (:body (app {:request-method :get :uri "/api/rehearsal"})))
          rehearsal-id (->> rehearsals (filter :is-open) first :id)

          ;; add an entry
          entry-resp (app (post-json-request (str "/api/rehearsal/" rehearsal-id "/entry")
                                           {:exercise-id exercise-id
                                            :variant-id variant-id
                                            :entry-time 1760110000
                                            :remarks "Original entry"}))
          _ (assert-status! entry-resp 200)

          ;; fetch the entry to get its ID
          entries (read-json-value (:body (app {:request-method :get
                                               :uri (str "/api/rehearsal/" rehearsal-id "/entry")})))
          entry (first entries)
          entry-id (:id entry)

          ;; Test 1: GET duplicate confirmation page should work
          dup-get-resp (app {:request-method :get
                            :uri (str "/rehearsals/" rehearsal-id "/entry/" entry-id "/duplicate.html")})
          _ (t/is (= 200 (:status dup-get-resp)) "Duplicate page GET should return 200")
          _ (t/is (string/includes? (:body dup-get-resp) "Duplicate entry for Tune to Duplicate")
                 "Duplicate page should show exercise title")

          ;; Test 2: POST to duplicate should create a new entry
          dup-post-resp (app {:request-method :post
                             :uri (str "/rehearsals/" rehearsal-id "/entry/" entry-id "/duplicate.html")})
          _ (t/is (= 303 (:status dup-post-resp)) "Duplicate POST should redirect")

          ;; Extract the new entry ID from the redirect location
          location (get-in dup-post-resp [:headers "location"])
          _ (t/is (clojure.string/includes? location "/entry/") "Redirect should go to entry page")
          ;; Extract new entry ID from location like "/rehearsals/123/entry/456/entry.html"
          parts (string/split location #"/")
          new-entry-id (Integer/parseInt (nth parts 4))

          ;; Test 3: Verify the new entry exists and has correct data
          new-entry-resp (app {:request-method :get
                              :uri (str "/api/rehearsal/" rehearsal-id "/entry")})
          new-entries (read-json-value (:body new-entry-resp))
          new-entry (some #(when (= (:id %) new-entry-id) %) new-entries)
          _ (t/is (not (nil? new-entry)) "New entry should exist")
          _ (t/is (= exercise-id (:exercise-id new-entry)) "New entry should have same exercise-id")
          _ (t/is (= variant-id (:variant-id new-entry)) "New entry should have same variant-id")
          _ (t/is (= "Original entry" (:remarks new-entry)) "New entry should have same remarks")
          _ (t/is (= 1760110000 (:entry-time new-entry)) "New entry should have same entry-time")
          _ (t/is (not= entry-id (:id new-entry)) "New entry should have different ID")

          ;; Test 4: Verify duplicate page for non-existent entry returns 404
          dup-404-resp (app {:request-method :get
                            :uri (str "/rehearsals/" rehearsal-id "/entry/999999/duplicate.html")})
          _ (t/is (= 404 (:status dup-404-resp)) "Duplicate page for non-existent entry should return 404")

          ;; Test 5: Verify duplicate POST for non-existent entry returns 404
          dup-post-404-resp (app {:request-method :post
                                  :uri (str "/rehearsals/" rehearsal-id "/entry/999999/duplicate.html")})
          _ (t/is (= 404 (:status dup-post-404-resp)) "Duplicate POST for non-existent entry should return 404")])))

(t/deftest rehearsal-three-entries-update-close-test
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies
                handler-explaining-400)]

    ;; sign up + login
    (app (post-form-request "/api/signup" {:username "alice" :password "pw"}))
    (app (post-form-request "/api/login" {:username "alice" :password "pw"}))

    ;; add three exercises
    (app (post-json-request "/api/exercise"
                            {:title "Kerry Reel" :description "Some reel"}))
    (app (post-json-request "/api/exercise"
                            {:title "Kerry Polka" :description "Some polka"}))
    (app (post-json-request "/api/exercise"
                            {:title "Kerry Jig" :description "Some jig"}))

    ;; fetch exercises and default variant id
    (let [exercises (read-json-value (:body (app {:request-method :get :uri "/api/exercise"})))
          variants (read-json-value (:body (app {:request-method :get :uri "/api/variant"})))
          default-variant-id (:id (first variants))]

      ;; create rehearsal
      (let [resp (app (post-json-request "/api/rehearsal"
                                         {:start-time 1760200000
                                          :duration nil
                                          :title "Afternoon session"
                                          :description "Focusing"}))]
        (assert-status! resp 200))

      ;; locate the open rehearsal
      (let [rehearsals (read-json-value (:body (app {:request-method :get :uri "/api/rehearsal"})))
            rehearsal-id (->> rehearsals (filter :is-open) first :id)]

        ;; add three entries (one per exercise)
        (doseq [[ex idx] (map vector exercises (range 3))]
          (let [resp (app (post-json-request (str "/api/rehearsal/" rehearsal-id "/entry")
                                             {:exercise-id (:id ex)
                                              :variant-id default-variant-id
                                              :entry-time (+ 1760200000 (* 600 idx))
                                              :remarks (str "Initial remark " (inc idx))}))]
            (assert-status! resp 200)))

        ;; verify three entries present
        (let [entries (read-json-value
                       (:body (app {:request-method :get
                                    :uri (str "/api/rehearsal/" rehearsal-id "/entry")})))]
          (t/is (= 3 (count entries)))

          ;; pick the second entry and update its remarks
          (let [second-entry-id (:id (second entries))
                update-entry-resp (app (put-json-request
                                        (str "/api/entry/" second-entry-id)
                                        {:remarks "Much improved on this one"}))]
            (t/is (= 200 (:status update-entry-resp))))

          ;; update rehearsal title and close it (set duration)
          (let [update-rehearsal-resp (app (put-json-request
                                            (str "/api/rehearsal/" rehearsal-id)
                                            {:title "Evening tune-up" :duration 2000}))]
            (t/is (= 200 (:status update-rehearsal-resp))))

          ;; fetch rehearsal and entries again and assert changes
          (let [rehearsal-after (read-json-value
                                 (:body (app {:request-method :get
                                              :uri (str "/api/rehearsal/" rehearsal-id)})))
                entries-after (read-json-value
                               (:body (app {:request-method :get
                                            :uri (str "/api/rehearsal/" rehearsal-id "/entry")})))
                second-entry-after (some #(when (= (:id %) (-> entries second :id)) %) entries-after)]

            ;; rehearsal title updated and rehearsal is closed
            (t/is (= "Evening tune-up" (:title rehearsal-after)))
            (t/is (not (:is-open rehearsal-after)))

            ;; entry remarks updated
            (t/is (= "Much improved on this one" (:remarks second-entry-after)))))))))

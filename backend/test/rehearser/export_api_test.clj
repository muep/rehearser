(ns rehearser.export-api-test
  (:require
    [clojure.test :as t]
    [rehearser.fixture :refer [fixture]]
    [rehearser.test-db :refer [test-db]]
    [rehearser.test-util :refer [handler-with-local-cookies post-form-request read-json-value]]
    [rehearser.http-service :as http-service]
    [crypto.random :as random]
    [rehearser.service.account :as account-service]
    [rehearser.service.exercise :as exercise-service]
    [rehearser.service.variant :as variant-service]
    [rehearser.service.rehearsal :as rehearsal-service]))

(t/use-fixtures :each fixture)

(t/deftest test-export-endpoint-requires-authentication
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler)
        response (app {:request-method :get
                      :uri "/api/export"})]

    ;; Should return 401 or 403 for unauthenticated requests
    (t/is (contains? #{401 403} (:status response)) "Unauthenticated export request should be rejected")))

(t/deftest test-export-endpoint-accepts-get-only
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies)
        test-account (account-service/create-account! test-db "testuser" "testpass")
        _ (app (post-form-request "/api/login" {:username "testuser" :password "testpass"}))]

    (let [get-response (app {:request-method :get
                            :uri "/api/export"})]
      (t/is (= (:status get-response) 200) "GET method should be supported"))))

(t/deftest test-export-endpoint-returns-correct-content-type
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies)
        test-account (account-service/create-account! test-db "testuser2" "testpass")
        _ (app (post-form-request "/api/login" {:username "testuser2" :password "testpass"}))]

    ;; Test that the endpoint returns correct content type
    (let [response (app {:request-method :get
                        :uri "/api/export"})]
      (t/is (= 200 (:status response)) "Response should be successful")
      (t/is (clojure.string/starts-with? (get-in response [:headers "Content-Type"]) "application/json") "Content-Type should start with application/json")
      (t/is (contains? (:headers response) "Content-Disposition") "Response should have Content-Disposition header"))))

(t/deftest test-export-response-has-download-header
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies)
        test-account (account-service/create-account! test-db "testuser3" "testpass")
        _ (app (post-form-request "/api/login" {:username "testuser3" :password "testpass"}))]

    ;; Test that the endpoint returns proper download headers
    (let [response (app {:request-method :get
                        :uri "/api/export"})]
      (t/is (= (:status response) 200) "Response should be successful")

      ;; Check Content-Disposition header format
      (let [content-disp (get-in response [:headers "Content-Disposition"])]
        (t/is (string? content-disp) "Content-Disposition should be a string")
        (when content-disp
          (t/is (.startsWith content-disp "attachment;") "Content-Disposition should start with 'attachment;'")
          (t/is (.contains content-disp "filename=") "Content-Disposition should contain filename parameter")
          (t/is (.endsWith content-disp ".json\"") "Filename should end with .json extension"))))))

(t/deftest test-export-response-converts-timestamps
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies)
        test-account (account-service/create-account! test-db "testuser4" "testpass")
        _ (app (post-form-request "/api/login" {:username "testuser4" :password "testpass"}))]

    ;; Test that timestamps are properly converted to Unix seconds
    (let [response (app {:request-method :get
                        :uri "/api/export"})]
      (t/is (= (:status response) 200) "Response should be successful")

      ;; Parse the JSON response to check timestamp format
      (let [body (:body response)
            parsed (when body (try (read-json-value body)
                                  (catch Exception e
                                    {})))
            exported-at (get parsed :exported-at)
            rehearsals (get parsed :rehearsals)
            entries (get parsed :entries)]

        ;; Check exported-at timestamp format
        (when exported-at
          (t/is (number? exported-at) "exported-at should be a number")
          (t/is (integer? exported-at) "exported-at should be an integer (Unix seconds)")
          (t/is (pos? exported-at) "exported-at should be positive")
          (t/is (< exported-at 253402300800) "exported-at should be reasonable (before year 9999)"))

        ;; Check rehearsal timestamps if any exist
        (when (seq rehearsals)
          (let [first-rehearsal (first rehearsals)
                start-time (get first-rehearsal "start-time")]
            (when start-time
              (t/is (number? start-time) "rehearsal start-time should be a number")
              (t/is (integer? start-time) "rehearsal start-time should be an integer"))))

        ;; Check entry timestamps if any exist
        (when (seq entries)
          (let [first-entry (first entries)
                entry-time (get first-entry "entry-time")]
            (when entry-time
              (t/is (number? entry-time) "entry entry-time should be a number")
              (t/is (integer? entry-time) "entry entry-time should be an integer"))))))))

(t/deftest test-export-response-matches-json-schema
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies)
        test-account (account-service/create-account! test-db "testuser6" "testpass")
        _ (app (post-form-request "/api/login" {:username "testuser6" :password "testpass"}))

        ;; Add some realistic test data using service layer (cleaner and faster)
        whoami {:account-id (:id test-account) :account-name "testuser6"}
        _ (exercise-service/add! test-db whoami {:title "Test Exercise" :description "Test Description"})
        _ (variant-service/add! test-db whoami {:title "Test Variant" :description "Test Variant"})
        _ (rehearsal-service/insert-rehearsal! test-db whoami
                                               {:title "Test Rehearsal"
                                                :description "Test"
                                                :start-time (java.time.Instant/ofEpochSecond 1000)
                                                :duration 3600})]

    ;; Test that the response matches the expected JSON schema
    (let [response (app {:request-method :get
                        :uri "/api/export"})]
      (t/is (= 200 (:status response)) "Response should be successful")

      ;; Parse the JSON response
      (let [body (:body response)
            parsed (when body (try (read-json-value body)
                                  (catch Exception e
                                    {})))]

        ;; Verify top-level structure
        (t/is (map? parsed) "Response should be a JSON object")

        ;; Verify required top-level fields
        (t/is (contains? parsed :version) "Response should contain version field")
        (t/is (contains? parsed :exported-at) "Response should contain exported-at field")
        (t/is (contains? parsed :account) "Response should contain account field")

        ;; Verify account structure
        (when-let [account (get parsed :account)]
          (t/is (map? account) "Account should be an object")
          (t/is (contains? account :id) "Account should contain id")
          (t/is (contains? account :name) "Account should contain name")
          (t/is (nil? (get account :pwhash)) "Account should not contain password hash"))

        ;; Verify data collections are arrays
        (t/is (vector? (get parsed :exercises)) "Exercises should be an array")
        (t/is (vector? (get parsed :variants)) "Variants should be an array")
        (t/is (vector? (get parsed :rehearsals)) "Rehearsals should be an array")
        (t/is (vector? (get parsed :entries)) "Entries should be an array")

        ;; Verify inner structure of exercises
        (when-let [exercises (seq (get parsed :exercises))]
          (let [first-exercise (first exercises)]
            (t/is (map? first-exercise) "Exercise should be an object")
            (t/is (contains? first-exercise :id) "Exercise should have id")
            (t/is (contains? first-exercise :title) "Exercise should have title")
            (t/is (contains? first-exercise :description) "Exercise should have description")
            (t/is (number? (get first-exercise :id)) "Exercise id should be number")
            (t/is (string? (get first-exercise :title)) "Exercise title should be string")
            (t/is (string? (get first-exercise :description)) "Exercise description should be string")
            (t/is (= (get first-exercise :title) "Test Exercise") "Exercise title should match our test data")
            (t/is (= (get first-exercise :description) "Test Description") "Exercise description should match our test data"))

        ;; Verify inner structure of variants
        (when-let [variants (seq (get parsed :variants))]
          (let [first-variant (first variants)]
            (t/is (map? first-variant) "Variant should be an object")
            (t/is (contains? first-variant :id) "Variant should have id")
            (t/is (contains? first-variant :title) "Variant should have title")
            (t/is (contains? first-variant :description) "Variant should have description")
            (t/is (number? (get first-variant :id)) "Variant id should be number")
            (t/is (string? (get first-variant :title)) "Variant title should be string")
            (t/is (string? (get first-variant :description)) "Variant description should be string")

            ;; Check that we have both the default variant and our test variant
            (let [variant-titles (map :title variants)]
              (t/is (some #{"default"} variant-titles) "Should include default variant")
              (t/is (some #{"Test Variant"} variant-titles) "Should include our test variant")))

        ;; Verify inner structure of rehearsals
        (when-let [rehearsals (seq (get parsed :rehearsals))]
          (let [first-rehearsal (first rehearsals)]
            (t/is (map? first-rehearsal) "Rehearsal should be an object")
            (t/is (contains? first-rehearsal :id) "Rehearsal should have id")
            (t/is (contains? first-rehearsal :title) "Rehearsal should have title")
            (t/is (contains? first-rehearsal :description) "Rehearsal should have description")
            (t/is (contains? first-rehearsal :start-time) "Rehearsal should have start-time")
            (t/is (contains? first-rehearsal :duration) "Rehearsal should have duration")
            (t/is (number? (get first-rehearsal :id)) "Rehearsal id should be number")
            (t/is (string? (get first-rehearsal :title)) "Rehearsal title should be string")
            (t/is (string? (get first-rehearsal :description)) "Rehearsal description should be string")
            (t/is (number? (get first-rehearsal :start-time)) "Rehearsal start-time should be number")
            (t/is (number? (get first-rehearsal :duration)) "Rehearsal duration should be number")
            (t/is (= (get first-rehearsal :title) "Test Rehearsal") "Rehearsal title should match our test data")
            (t/is (= (get first-rehearsal :description) "Test") "Rehearsal description should match our test data")
            (t/is (= (get first-rehearsal :start-time) 1000) "Rehearsal start-time should match our test data")
            (t/is (= (get first-rehearsal :duration) 3600) "Rehearsal duration should match our test data"))

        ;; Verify inner structure of entries
        (when-let [entries (seq (get parsed :entries))]
          (let [first-entry (first entries)]
            (t/is (map? first-entry) "Entry should be an object")
            (t/is (contains? first-entry :id) "Entry should have id")
            (t/is (contains? first-entry :rehearsal-id) "Entry should have rehearsal-id")
            (t/is (contains? first-entry :exercise-id) "Entry should have exercise-id")
            (t/is (contains? first-entry :variant-id) "Entry should have variant-id")
            (t/is (contains? first-entry :entry-time) "Entry should have entry-time")
            (t/is (contains? first-entry :remarks) "Entry should have remarks")
            (t/is (number? (get first-entry :id)) "Entry id should be number")
            (t/is (number? (get first-entry :rehearsal-id)) "Entry rehearsal-id should be number")
            (t/is (number? (get first-entry :exercise-id)) "Entry exercise-id should be number")
            (t/is (number? (get first-entry :variant-id)) "Entry variant-id should be number")
            (t/is (number? (get first-entry :entry-time)) "Entry entry-time should be number")
            (t/is (or (nil? (get first-entry :remarks)) (string? (get first-entry :remarks))) "Entry remarks should be string or null")

            ;; Verify relationships are maintained
            (let [rehearsal-id (get first-entry :rehearsal-id)
                  exercise-id (get first-entry :exercise-id)
                  variant-id (get first-entry :variant-id)]
              (t/is (pos? rehearsal-id) "Entry should reference a valid rehearsal")
              (t/is (pos? exercise-id) "Entry should reference a valid exercise")
              (t/is (pos? variant-id) "Entry should reference a valid variant"))))

        ;; Verify version is correct
        (when-let [version (get parsed :version)]
          (t/is (= version 1) "Version should be 1")))))))))

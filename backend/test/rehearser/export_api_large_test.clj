(ns rehearser.export-api-large-test
  (:require [clojure.test :as t]
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

(t/deftest test-export-with-large-dataset
  (let [app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                :handler
                handler-with-local-cookies)
        test-account (account-service/create-account! test-db "testuser7" "testpass")
        _ (app (post-form-request "/api/login" {:username "testuser7" :password "testpass"}))
        whoami {:account-id (:id test-account) :account-name "testuser7"}

        ;; Create a larger dataset - 10 exercises, 10 variants, 5 rehearsals, 50 entries
        exercise-ids (doall (for [i (range 10)]
                              (:id (exercise-service/add! test-db whoami
                                                          {:title (str "Exercise " i)
                                                           :description (str "Description " i)}))))
        variant-ids (doall (for [i (range 10)]
                             (:id (variant-service/add! test-db whoami
                                                        {:title (str "Variant " i)
                                                         :description (str "Variant Desc " i)
                                                         :exercise-id (nth exercise-ids (mod i (count exercise-ids)))}))))
        rehearsal-ids (doall (for [i (range 5)]
                               (:id (rehearsal-service/insert-rehearsal! test-db whoami
                                                                     {:title (str "Rehearsal " i)
                                                                      :description (str "Rehearsal Desc " i)
                                                                      :start-time (java.time.Instant/ofEpochSecond (+ 1000 (* i 86400)))
                                                                      :duration 3600}))))

        ;; Create 50 entries distributed across rehearsals
        _ (doall (for [i (range 50)]
                   (rehearsal-service/insert-entry! test-db whoami
                                                 {:rehearsal-id (nth rehearsal-ids (mod i (count rehearsal-ids)))
                                                  :exercise-id (nth exercise-ids (mod i (count exercise-ids)))
                                                  :variant-id (nth variant-ids (mod i (count variant-ids)))
                                                  :entry-time (java.time.Instant/ofEpochSecond (+ 1000 (* i 60)))
                                                  :remarks (str "Entry content " i)})))

        request {:request-method :get
                :uri "/api/export"}
        response (app request)
        body (:body response)
        parsed (read-json-value body)]

    (t/is (= 200 (:status response)) "Response should be successful")
    (t/is (map? parsed) "Parsed JSON should be a map")

    ;; Verify we have the expected quantities
    ;; Note: variants include the default variant, so 10 created + 1 default = 11 total
    (t/is (= (count (get parsed :exercises [])) 10) "Should have 10 exercises")
    (t/is (= (count (get parsed :variants [])) 11) "Should have 11 variants (10 created + 1 default)")
    (t/is (= (count (get parsed :rehearsals [])) 5) "Should have 5 rehearsals")
    (t/is (= (count (get parsed :entries [])) 50) "Should have 50 entries")

    ;; Verify account information
    (when-let [account (get parsed :account)]
      (t/is (map? account) "Account should be an object")
      (t/is (number? (get account :id)) "Account id should be number")
      (t/is (string? (get account :name)) "Account name should be string")
      (t/is (= (get account :name) "testuser7") "Account name should match our test data"))

    ;; Verify first exercise has correct structure
    (when-let [exercises (seq (get parsed :exercises))]
      (let [first-exercise (first exercises)]
        (t/is (map? first-exercise) "First exercise should be an object")
        (t/is (number? (get first-exercise :id)) "Exercise id should be number")
        (t/is (string? (get first-exercise :title)) "Exercise title should be string")
        (t/is (string? (get first-exercise :description)) "Exercise description should be string")))

    ;; Verify first variant has correct structure and relationships
    (when-let [variants (seq (get parsed :variants))]
      (let [first-variant (first variants)]
        (t/is (map? first-variant) "First variant should be an object")
        (t/is (number? (get first-variant :id)) "Variant id should be number")
        (t/is (string? (get first-variant :title)) "Variant title should be string")
        (t/is (string? (get first-variant :description)) "Variant description should be string")))

    ;; Verify first rehearsal has correct structure
    (when-let [rehearsals (seq (get parsed :rehearsals))]
      (let [first-rehearsal (first rehearsals)]
        (t/is (map? first-rehearsal) "First rehearsal should be an object")
        (t/is (number? (get first-rehearsal :id)) "Rehearsal id should be number")
        (t/is (string? (get first-rehearsal :title)) "Rehearsal title should be string")
        (t/is (number? (get first-rehearsal :start-time)) "Rehearsal start-time should be number")
        (t/is (number? (get first-rehearsal :duration)) "Rehearsal duration should be number")))

    ;; Verify first entry has correct structure and relationships
    (when-let [entries (seq (get parsed :entries))]
      (let [first-entry (first entries)]
        (t/is (map? first-entry) "First entry should be an object")
        (t/is (number? (get first-entry :id)) "Entry id should be number")
        (t/is (string? (get first-entry :remarks)) "Entry remarks should be string")
        (t/is (number? (get first-entry :entry-time)) "Entry entry-time should be number")
        (t/is (number? (get first-entry :rehearsal-id)) "Entry should reference a rehearsal")
        (t/is (number? (get first-entry :exercise-id)) "Entry should reference an exercise")
        (t/is (number? (get first-entry :variant-id)) "Entry should reference a variant")

        ;; Verify relationships are maintained
        (let [rehearsal-id (get first-entry :rehearsal-id)
              exercise-id (get first-entry :exercise-id)
              variant-id (get first-entry :variant-id)]
          (t/is (pos? rehearsal-id) "Entry should reference a valid rehearsal")
          (t/is (pos? exercise-id) "Entry should reference a valid exercise")
          (t/is (pos? variant-id) "Entry should reference a valid variant"))))))

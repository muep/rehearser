(ns rehearser.export-test
  (:require
    [clojure.test :as t]
    [rehearser.fixture :refer [fixture]]
    [rehearser.test-db :refer [test-db]]
    [rehearser.test-util :refer [handler-with-local-cookies
                                 post-form-request
                                 post-json-request
                                 read-json-value]]
    [rehearser.http-service :as http-service]
    [crypto.random :as random]
    [jsonista.core :as json]
    [ring.mock.request :as mock]
    [rehearser.service.account :as account-service]
    [rehearser.service.exercise :as exercise-service]
    [rehearser.service.rehearsal :as rehearsal-service]
    [rehearser.service.variant :as variant-service])
  (:import (java.io ByteArrayInputStream)))

(t/use-fixtures :each fixture)

(t/deftest test-export-import-roundtrip
  "Test the complete export-import workflow with realistic data"
  (let
      [exported-data
       (let [;; Create source account and login
             source-username "sourceuser"
             source-password "source-pass"
             source-account (account-service/create-account! test-db source-username source-password)


             ;; Create test data in source account
             whoami {:account-id (:id source-account) :account-name source-username}
             exercise1 (exercise-service/add! test-db whoami {:title "Test Exercise 1" :description "First test exercise"})
             exercise2 (exercise-service/add! test-db whoami {:title "Test Exercise 2" :description "Second test exercise"})

             ;; Get the default variant (comes with account)
             default-variant (first (variant-service/find-all test-db whoami))

             rehearsal1 (rehearsal-service/insert-rehearsal! test-db whoami
                                                             {:title "Test Rehearsal"
                                                              :description "Test rehearsal for export-import"
                                                              :start-time (java.time.Instant/ofEpochSecond 1000)
                                                              :duration 3600})

             ;; Create entries using the default variant
             entry1 (rehearsal-service/insert-entry! test-db whoami
                                                     {:rehearsal-id (:id rehearsal1)
                                                      :exercise-id (:id exercise1)
                                                      :variant-id (:id default-variant)
                                                      :entry-time (java.time.Instant/ofEpochSecond 1100)
                                                      :remarks "First test entry"})

             entry2 (rehearsal-service/insert-entry! test-db whoami
                                                     {:rehearsal-id (:id rehearsal1)
                                                      :exercise-id (:id exercise2)
                                                      :variant-id (:id default-variant)
                                                      :entry-time (java.time.Instant/ofEpochSecond 1200)
                                                      :remarks "Second test entry"})

             app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                     :handler
                     handler-with-local-cookies)
             login-response (app (post-form-request "/api/login" {:username source-username :password source-password}))

             ;; Export the data
             export-response (app {:request-method :get
                                   :uri "/api/export"})]
         (t/is (= 303 (:status login-response)))
         (t/is (= 200 (:status export-response)) "Export should succeed")
         (when-let [body (:body export-response)]
           (read-json-value body)))]

    (t/is (map? exported-data) "Exported data should be a map")
    (t/is (= 1 (:version exported-data)) "Version should be 1")
    (t/is (number? (:exported-at exported-data)) "Should have exported-at timestamp")

    ;; Verify exported data structure
    (t/is (= 2 (count (:exercises exported-data))) "Should export 2 exercises")
    (t/is (= 1 (count (:variants exported-data))) "Should export 1 default variant")
    (t/is (= 1 (count (:rehearsals exported-data))) "Should export 1 rehearsal")
    (t/is (= 2 (count (:entries exported-data))) "Should export 2 entries")

    (let [username "targetuser"
          password "targetpwd"
          target-account (account-service/create-account! test-db username password)
          target-whoami {:account-id (:id target-account) :account-name username}
          app (-> (http-service/make-app test-db (random/bytes 16) "" nil nil)
                  :handler
                  handler-with-local-cookies)]

      (app (post-form-request "/api/login" {:username username :password password}))

      ;; Import the exported data into target account using multipart endpoint
      (let [json-content (json/write-value-as-string exported-data)
            boundary "----WebKitFormBoundary7MA4YWxkTrZu0gW"
            body-text (str "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                          "Content-Disposition: form-data; name=\"file\"; filename=\"export.json\"\r\n"
                          "Content-Type: application/json\r\n\r\n"
                          json-content "\r\n"
                          "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n")
            import-request (-> (mock/request :post "/import.html")
                              (mock/content-type "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
                              (mock/body body-text))
            import-response (app import-request)]
        (t/is (= 303 (:status import-response)) "Import should redirect after success"))

      ;; Verify data was imported into target account
      (let [imported-exercises (exercise-service/find-all test-db target-whoami)
            imported-variants (variant-service/find-all test-db target-whoami)
            imported-rehearsals (rehearsal-service/find-all test-db target-whoami)
            imported-entries (rehearsal-service/find-entries-of-rehearsal test-db target-whoami (:id (first imported-rehearsals)))]
        ;; Verify counts match
        (t/is (= 2 (count imported-exercises)) "Should have imported 2 exercises")
        (t/is (= 1 (count imported-variants)) "Should have imported 1 variant (default)")
        (t/is (= 1 (count imported-rehearsals)) "Should have imported 1 rehearsal")
        (t/is (= 2 (count imported-entries)) "Should have imported 2 entries")

        ;; Verify content matches (titles, descriptions)
        (let [imported-exercise1 (first (filter #(= "Test Exercise 1" (:title %)) imported-exercises))
              imported-exercise2 (first (filter #(= "Test Exercise 2" (:title %)) imported-exercises))
              imported-rehearsal (first imported-rehearsals)
              imported-entry1 (first (filter #(= "First test entry" (:remarks %)) imported-entries))
              imported-entry2 (first (filter #(= "Second test entry" (:remarks %)) imported-entries))
              imported-variant (first imported-variants)]
          (t/is (= "Test Exercise 1" (:title imported-exercise1)) "First exercise title should match")
          (t/is (= "First test exercise" (:description imported-exercise1)) "First exercise description should match")
          (t/is (= "Test Exercise 2" (:title imported-exercise2)) "Second exercise title should match")
          (t/is (= "Second test exercise" (:description imported-exercise2)) "Second exercise description should match")
          (t/is (= "Test Rehearsal" (:title imported-rehearsal)) "Rehearsal title should match")
          (t/is (= "Test rehearsal for export-import" (:description imported-rehearsal)) "Rehearsal description should match")
          (t/is (= "First test entry" (:remarks imported-entry1)) "First entry remarks should match")
          (t/is (= "Second test entry" (:remarks imported-entry2)) "Second entry remarks should match")
          (t/is (= "default" (:title imported-variant)) "Variant title should match"))

        ;; Verify relationships are preserved
        (let [imported-exercise1 (first (filter #(= "Test Exercise 1" (:title %)) imported-exercises))
              imported-exercise2 (first (filter #(= "Test Exercise 2" (:title %)) imported-exercises))
              imported-rehearsal (first imported-rehearsals)
              imported-entry1 (first (filter #(= "First test entry" (:remarks %)) imported-entries))
              imported-entry2 (first (filter #(= "Second test entry" (:remarks %)) imported-entries))
              imported-variant (first imported-variants)]
          ;; Verify entry relationships
          (t/is (= (:id imported-exercise1) (:exercise-id imported-entry1)) "Entry1 should reference exercise1")
          (t/is (= (:id imported-exercise2) (:exercise-id imported-entry2)) "Entry2 should reference exercise2")
          (t/is (= (:id imported-rehearsal) (:rehearsal-id imported-entry1)) "Entry1 should reference the rehearsal")
          (t/is (= (:id imported-rehearsal) (:rehearsal-id imported-entry2)) "Entry2 should reference the rehearsal")
          (t/is (= (:id imported-variant) (:variant-id imported-entry1)) "Entry1 should reference the variant")
          (t/is (= (:id imported-variant) (:variant-id imported-entry2)) "Entry2 should reference the variant")
          ;; Verify all entries belong to the same rehearsal
          (t/is (every? #(= (:id imported-rehearsal) (:rehearsal-id %)) imported-entries) "All entries should belong to the same rehearsal"))

        ;; Verify IDs are different (new IDs assigned during import)
        (let [original-exercise-ids (set (map :id (:exercises exported-data)))
              original-rehearsal-ids (set (map :id (:rehearsals exported-data)))
              original-entry-ids (set (map :id (:entries exported-data)))
              original-variant-ids (set (map :id (:variants exported-data)))
              imported-exercise-ids (set (map :id imported-exercises))
              imported-rehearsal-ids (set (map :id imported-rehearsals))
              imported-entry-ids (set (map :id imported-entries))
              imported-variant-ids (set (map :id imported-variants))]
          (t/is (not-any? original-exercise-ids imported-exercise-ids) "Imported exercises should have new IDs")
          (t/is (not-any? original-rehearsal-ids imported-rehearsal-ids) "Imported rehearsals should have new IDs")
          (t/is (not-any? original-entry-ids imported-entry-ids) "Imported entries should have new IDs")
          (t/is (not-any? original-variant-ids imported-variant-ids) "Imported variants should have new IDs"))))))

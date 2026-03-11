(ns rehearser.export-service-test
  (:require
    [clojure.test :as t]
    [rehearser.fixture :refer [fixture]]
    [rehearser.test-db :refer [test-db]]
    [next.jdbc :as jdbc]
    [rehearser.service.account :as account-service]
    [rehearser.service.exercise :as exercise-service]
    [rehearser.service.variant :as variant-service]
    [rehearser.service.rehearsal :as rehearsal-service]
    [rehearser.service.export :as export-service])
  (:import (java.time Instant)))

(t/use-fixtures :each fixture)

(t/deftest test-export-account-basic-functionality
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        test-account (account-service/create-account! db-conn "testuser" "testpass")
        whoami {:account-id (:id test-account) :account-name "testuser"}

        ;; Create some exercises
        exercise1 (exercise-service/add! db-conn whoami {:title "Scale Practice" :description "Major scales"})
        exercise2 (exercise-service/add! db-conn whoami {:title "Arpeggio Practice" :description "Major arpeggios"})

        ;; Create some variants
        variant1 (variant-service/add! db-conn whoami {:title "Slow tempo" :description "60 BPM"})
        variant2 (variant-service/add! db-conn whoami {:title "Fast tempo" :description "120 BPM"})

        ;; Create a rehearsal
        rehearsal1 (rehearsal-service/insert-rehearsal! db-conn whoami
                                                       {:title "Morning Practice"
                                                        :description "Daily routine"
                                                        :start-time (java.time.Instant/ofEpochSecond 1000)
                                                        :duration 3600})

        ;; Create some entries using the exercises and variants
        entry1 (rehearsal-service/insert-entry! db-conn whoami
                                               {:rehearsal-id (:id rehearsal1)
                                                :exercise-id (:id exercise1)
                                                :variant-id (:id variant1)
                                                :entry-time (java.time.Instant/ofEpochSecond 1100)
                                                :remarks "Good progress"})

        entry2 (rehearsal-service/insert-entry! db-conn whoami
                                               {:rehearsal-id (:id rehearsal1)
                                                :exercise-id (:id exercise2)
                                                :variant-id (:id variant2)
                                                :entry-time (java.time.Instant/ofEpochSecond 1200)
                                                :remarks "Needs more work"})

        export-data (export-service/export-account db-conn (:id test-account))]

    ;; Basic structure validation
    (t/is (map? export-data) "Export data should be a map")
    (t/is (= (:version export-data) 1) "Export version should be 1")
    (t/is (instance? java.time.Instant (:exported-at export-data)) "exported-at should be an Instant")

    ;; Account validation
    (t/is (contains? export-data :account) "Export should contain account")
    (t/is (= (:id (:account export-data)) (:id test-account)) "Account ID should match")
    (t/is (= (:name (:account export-data)) "testuser") "Account name should match")
    (t/is (nil? (:pwhash (:account export-data))) "Account should not contain password hash")

    ;; Exercises validation
    (t/is (contains? export-data :exercises) "Export should contain exercises")
    (t/is (= (count (:exercises export-data)) 2) "Should export 2 exercises")

    ;; Variants validation (2 created + 1 default = 3 total)
    (t/is (contains? export-data :variants) "Export should contain variants")
    (t/is (= (count (:variants export-data)) 3) "Should export 3 variants (2 created + 1 default)")

    ;; Rehearsals validation
    (t/is (contains? export-data :rehearsals) "Export should contain rehearsals")
    (t/is (= (count (:rehearsals export-data)) 1) "Should export 1 rehearsal")

    ;; Entries validation
    (t/is (contains? export-data :entries) "Export should contain entries")
    (t/is (= (count (:entries export-data)) 2) "Should export 2 entries")

    ;; Verify relationships are preserved
    ;; Note: variant IDs are offset by 1 due to default variant (ID 1)
    ;; variant1 will have ID 2, variant2 will have ID 3
    (let [entries (:entries export-data)
          entry1-exported (first entries)
          entry2-exported (second entries)]

      (t/is (= (:exercise-id entry1-exported) (:id exercise1)) "First entry should reference correct exercise")
      (t/is (= (:variant-id entry1-exported) (:id variant1)) "First entry should reference correct variant")
      (t/is (= (:rehearsal-id entry1-exported) (:id rehearsal1)) "First entry should reference correct rehearsal")

      (t/is (= (:exercise-id entry2-exported) (:id exercise2)) "Second entry should reference correct exercise")
      (t/is (= (:variant-id entry2-exported) (:id variant2)) "Second entry should reference correct variant")
      (t/is (= (:rehearsal-id entry2-exported) (:id rehearsal1)) "Second entry should reference correct rehearsal"))))

(t/deftest test-export-account-validates-account-exists
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        non-existent-account-id 999999]
    (t/is (thrown? Exception (export-service/export-account db-conn non-existent-account-id)) "Should throw exception for non-existent account")))

(t/deftest test-export-account-handles-empty-data
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        test-account (account-service/create-account! db-conn "emptyuser" "testpass")
        export-data (export-service/export-account db-conn (:id test-account))]

    ;; Should still have basic structure even with no data
    (t/is (map? export-data) "Export data should be a map")
    (t/is (= (:version export-data) 1) "Export version should be 1")
    (t/is (instance? java.time.Instant (:exported-at export-data)) "exported-at should be an Instant")

    ;; Account should be present
    (t/is (contains? export-data :account) "Export should contain account")
    (t/is (= (:id (:account export-data)) (:id test-account)) "Account ID should match")

    ;; Other collections should be empty but present
    (t/is (contains? export-data :exercises) "Export should contain exercises")
    (t/is (empty? (:exercises export-data)) "Exercises should be empty")

    (t/is (contains? export-data :variants) "Export should contain variants")
    (t/is (= (count (:variants export-data)) 1) "Variants should contain the default variant")

    (t/is (contains? export-data :rehearsals) "Export should contain rehearsals")
    (t/is (empty? (:rehearsals export-data)) "Rehearsals should be empty")

    (t/is (contains? export-data :entries) "Export should contain entries")
    (t/is (empty? (:entries export-data)) "Entries should be empty")))

(t/deftest test-export-account-excludes-password-hash
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        test-account (account-service/create-account! db-conn "testuser5" "testpass")
        whoami {:account-id (:id test-account) :account-name "testuser5"}

        ;; Add some realistic data to make the test more comprehensive
        exercise1 (exercise-service/add! db-conn whoami {:title "Test Exercise" :description "Test"})
        rehearsal1 (rehearsal-service/insert-rehearsal! db-conn whoami
                                                       {:title "Test Rehearsal"
                                                        :description "Test"
                                                        :start-time (java.time.Instant/ofEpochSecond 500)
                                                        :duration 1800})
        export-data (export-service/export-account db-conn (:id test-account))]

    (t/is (contains? export-data :account) "Export should contain account")
    (let [account-data (:account export-data)]
      (t/is (nil? (:pwhash account-data)) "Account should not contain password hash")
      (t/is (nil? (get account-data "pwhash")) "Account should not contain password hash (string key)")
      (t/is (contains? account-data :id) "Account should contain id")
      (t/is (contains? account-data :name) "Account should contain name"))

    ;; Verify other data is still exported correctly
    (t/is (= (count (:exercises export-data)) 1) "Should export exercise")
    (t/is (= (count (:rehearsals export-data)) 1) "Should export rehearsal")))

(t/deftest test-export-account-includes-version
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        test-account (account-service/create-account! db-conn "testuser7" "testpass")
        whoami {:account-id (:id test-account) :account-name "testuser7"}

        ;; Add realistic data
        variant1 (variant-service/add! db-conn whoami {:title "Test Variant" :description "Test"})
        export-data (export-service/export-account db-conn (:id test-account))]

    (t/is (contains? export-data :version) "Export should have version field")
    (t/is (= (:version export-data) 1) "Export version should be 1")

    ;; Verify other data is exported too (1 created + 1 default = 2 total)
    (t/is (= (count (:variants export-data)) 2) "Should export variants (1 created + 1 default)")))

(t/deftest test-export-account-includes-exported-at
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        test-account (account-service/create-account! db-conn "testuser8" "testpass")
        whoami {:account-id (:id test-account) :account-name "testuser8"}

        ;; Add realistic data
        exercise1 (exercise-service/add! db-conn whoami {:title "Test Exercise" :description "Test"})
        rehearsal1 (rehearsal-service/insert-rehearsal! db-conn whoami
                                                       {:title "Test Rehearsal"
                                                        :description "Test"
                                                        :start-time (java.time.Instant/ofEpochSecond 1000)
                                                        :duration 1800})
        variant1 (variant-service/add! db-conn whoami {:title "Test Variant" :description "Test"})
        entry1 (rehearsal-service/insert-entry! db-conn whoami
                                               {:rehearsal-id (:id rehearsal1)
                                                :exercise-id (:id exercise1)
                                                :variant-id (:id variant1)
                                                :entry-time (java.time.Instant/ofEpochSecond 2000)
                                                :remarks "Test entry"})
        export-data (export-service/export-account db-conn (:id test-account))]

    (t/is (contains? export-data :exported-at) "Export should have exported-at field")
    (t/is (instance? java.time.Instant (:exported-at export-data)) "exported-at should be an Instant")

    ;; Verify other data is exported too
    (t/is (= (count (:exercises export-data)) 1) "Should export exercise")
    (t/is (= (count (:variants export-data)) 2) "Should export variants (1 created + 1 default)")
    (t/is (= (count (:rehearsals export-data)) 1) "Should export rehearsal")
    (t/is (= (count (:entries export-data)) 1) "Should export entry")))

(t/deftest test-export-account-handles-null-duration
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        test-account (account-service/create-account! db-conn "testuser3" "testpass")
        whoami {:account-id (:id test-account) :account-name "testuser3"}

        ;; Create exercise and variant for more realistic test
        exercise1 (exercise-service/add! db-conn whoami {:title "Test Exercise" :description "Test"})
        variant1 (variant-service/add! db-conn whoami {:title "Test Variant" :description "Test"})

        rehearsal1 (rehearsal-service/insert-rehearsal! db-conn whoami
                                                       {:title "Rehearsal with null duration"
                                                        :description "Test"
                                                        :start-time (java.time.Instant/ofEpochSecond 0)
                                                        :duration nil})

        ;; Add an entry to make it more realistic
        entry1 (rehearsal-service/insert-entry! db-conn whoami
                                               {:rehearsal-id (:id rehearsal1)
                                                :exercise-id (:id exercise1)
                                                :variant-id (:id variant1)
                                                :entry-time (java.time.Instant/ofEpochSecond 100)
                                                :remarks "Test entry"})

        export-data (export-service/export-account db-conn (:id test-account))]

    (t/is (= (count (:rehearsals export-data)) 1) "Should export rehearsal")
    (let [exported-rehearsal (first (:rehearsals export-data))]
      (t/is (nil? (:duration exported-rehearsal)) "Rehearsal duration should be nil")

      ;; Verify related data is also exported
      (t/is (= (count (:exercises export-data)) 1) "Should export exercise")
      (t/is (= (count (:variants export-data)) 2) "Should export variants (1 created + 1 default)")
      (t/is (= (count (:entries export-data)) 1) "Should export entry"))))

(t/deftest test-export-account-preserves-relationships
  (let [db-conn (jdbc/get-datasource {:jdbcUrl test-db})
        test-account (account-service/create-account! db-conn "testuser4" "testpass")
        whoami {:account-id (:id test-account) :account-name "testuser4"}

        ;; Create two rehearsals with different durations to avoid constraint violation
        rehearsal1 (rehearsal-service/insert-rehearsal! db-conn whoami
                                                       {:title "Rehearsal 1"
                                                        :description "Desc 1"
                                                        :start-time (java.time.Instant/ofEpochSecond 1000)
                                                        :duration 3600})  ;; 1 hour duration
        rehearsal2 (rehearsal-service/insert-rehearsal! db-conn whoami
                                                       {:title "Rehearsal 2"
                                                        :description "Desc 2"
                                                        :start-time (java.time.Instant/ofEpochSecond 2000)
                                                        :duration 1800})  ;; 30 minute duration

        export-data (export-service/export-account db-conn (:id test-account))]

    ;; Verify that data is preserved correctly
    (t/is (= (count (:rehearsals export-data)) 2) "Should export 2 rehearsals")

    ;; Verify rehearsal IDs and other properties are preserved
    (let [rehearsals (:rehearsals export-data)
          rehearsal1-exported (first rehearsals)
          rehearsal2-exported (second rehearsals)]

      (t/is (= (:id rehearsal1-exported) (:id rehearsal1)) "First rehearsal ID should match")
      (t/is (= (:id rehearsal2-exported) (:id rehearsal2)) "Second rehearsal ID should match")

      ;; Verify titles are preserved
      (t/is (= (:title rehearsal1-exported) "Rehearsal 1") "First rehearsal title should match")
      (t/is (= (:title rehearsal2-exported) "Rehearsal 2") "Second rehearsal title should match")

      ;; Verify durations are preserved
      (t/is (= (:duration rehearsal1-exported) 3600) "First rehearsal duration should match")
      (t/is (= (:duration rehearsal2-exported) 1800) "Second rehearsal duration should match"))))

(ns rehearser.bcrypt-test
  (:require
   [clojure.test :as t])
  (:import
   (org.springframework.security.crypto.bcrypt BCrypt)))

(def password "cT9vKz2mQp7xWr4nFj6h")

(def pwhash-5-8-2
  "$2a$12$fGqpeuBiRucrlEHDdl/Hve4Wyy5b4HpJrV6SAELJ.aEGNO71p172a")

(t/deftest existing-pwhash-verifies-test
  (t/is (true? (BCrypt/checkpw password pwhash-5-8-2))))

(t/deftest wrong-password-rejected-test
  (t/is (false? (BCrypt/checkpw "wrong-password" pwhash-5-8-2))))

(t/deftest freshly-hashed-pwhash-verifies-test
  (let [pwhash (BCrypt/hashpw password (BCrypt/gensalt 12))]
    (t/is (true? (BCrypt/checkpw password pwhash)))))

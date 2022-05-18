(ns rehearser.api.account
  (:require [jeesql.core :refer [defqueries]])
  (:import (org.springframework.security.crypto.bcrypt BCrypt)))

(defqueries "rehearser/account.sql")

(defn whoami [{:keys [session]}]
  {:status 200
   :body (if (not (empty? session))
           (select-keys session [:account-id
                                 :account-name])
           {:account-id nil
            :account-name "anonymous"})})

(defn login [{:keys [db session]
              {:strs [username password]} :params}]
  (if-let [{:keys [id pwhash]} (first
                                (select-account-by-name db
                                                        {:name username}))]
    (if (BCrypt/checkpw password pwhash)
      {:status 200
       :session (assoc session
                       :account-id id
                       :account-name username)
       :body "Did log in"}
      {:status 401
       :body "Password check failed"})
    {:status 404
     :body "Did not find the user"}))

(def logout login)

(def passwd login)

(def signup login)
